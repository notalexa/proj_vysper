/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.extension.xep0114.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.extension.xep0114.ExternalComponent;
import org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg.InBandRegistrationHandler;
import org.apache.vysper.xmpp.protocol.ResponseWriter;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.protocol.StateAwareProtocolWorker;
import org.apache.vysper.xmpp.protocol.EvalHandler;
import org.apache.vysper.xmpp.protocol.worker.AuthenticatedProtocolWorker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * responsible for high-level XMPP protocol logic for client-server sessions
 * determines start, end and jabber conditions.
 * reads the stream and cuts it into stanzas,
 * holds state and invokes stanza execution,
 * separates stream reading from actual execution.
 * stateless.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ProtocolWorker implements StanzaProcessor {

    final Logger logger = LoggerFactory.getLogger(ProtocolWorker.class);

    private final Map<SessionState, StateAwareProtocolWorker> stateWorker = new HashMap<SessionState, StateAwareProtocolWorker>();

    private final ResponseWriter responseWriter = new ResponseWriter();
    
    ExternalComponent component;

    public ProtocolWorker(ExternalComponent component) {
    	this.component=component;
        stateWorker.put(SessionState.INITIATED, new InitiatedProtocolWorker());
        stateWorker.put(SessionState.AUTHENTICATED, new AuthenticatedProtocolWorker());
    }

    /**
     * executes the handler for a stanza, handles Protocol exceptions.
     * also writes a response, if the handler implements ResponseStanzaContainer
     * @param serverRuntimeContext
     * @param sessionContext
     * @param stanza
     * @param sessionStateHolder
     */
    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza,
            SessionStateHolder sessionStateHolder) {
        if (stanza == null)
            throw new RuntimeException("cannot process NULL stanzas");
        ServerRuntimeContext targetContext=sessionContext==null?serverRuntimeContext:sessionContext.resolveDomainContext(stanza);
        StanzaHandler stanzaHandler = targetContext.getHandler(stanza);
        if (stanzaHandler == null) {
            responseWriter.handleUnsupportedStanzaType(sessionContext, stanza);
            return;
        }
        if (sessionContext == null && stanzaHandler.isSessionRequired()) {
            throw new IllegalStateException("handler requires session context");
        }
        if(targetContext!=sessionContext.getServerRuntimeContext()) {
        	stanzaHandler=new EvalHandler(targetContext,stanzaHandler);
        }

        StateAwareProtocolWorker stateAwareProtocolWorker = stateWorker.get(sessionContext.getState());
        if (stateAwareProtocolWorker == null) {
            throw new IllegalStateException("no protocol worker for state " + sessionContext.getState().toString());
        }

        // check as of RFC3920/4.3:
        if (sessionStateHolder.getState() != SessionState.AUTHENTICATED) {
            // is not authenticated...
            if (XMPPCoreStanza.getWrapper(stanza) != null
                    && !(stanzaHandler instanceof InBandRegistrationHandler)) {
                // ... and is a IQ/PRESENCE/MESSAGE stanza!
                responseWriter.handleNotAuthorized(sessionContext, stanza);
                return;
            }
        }

        Entity from = stanza.getFrom();
        // make sure that 'from' (if present) matches the bare authorized entity
        // else respond with a stanza error 'unknown-sender'
        // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
        if (from != null && sessionContext.getInitiatingEntity() != null) {
            Entity fromBare = from.getBareJID();
            Entity initiatingEntity = sessionContext.getInitiatingEntity();
            if (!initiatingEntity.equals(fromBare)) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }
        // make sure that there is a bound resource entry for that from's resource id attribute!
        if (from != null && from.getResource() != null) {
            List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry()
                    .getBoundResources(from, false);
            if (boundResources.size() == 0) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }
        // make sure that there is a full from entity given in cases where more than one resource is bound
        // in the same session.
        // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
        if (from != null && from.getResource() == null) {
            List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry()
                    .getResourcesForSession(sessionContext);
            if (boundResources.size() > 1) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }
        
        try {
            stateAwareProtocolWorker.processStanza(sessionContext, sessionStateHolder, stanza, stanzaHandler);
        } catch (Exception e) {
            logger.error("error executing handler {} with stanza {}", stanzaHandler.getClass().getName(),
                    DenseStanzaLogRenderer.render(stanza));
            logger.debug("error executing handler exception: ", e);
        }
    }

	@Override
	public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
	}
}
