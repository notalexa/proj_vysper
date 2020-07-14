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
package org.apache.vysper.mina;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.vysper.mina.codec.StanzaWriteInfo;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContextProvider;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XmppIoHandlerAdapter implements IoHandler {

    public static final String ATTRIBUTE_VYSPER_SESSION = "vysperSession";

    public static final String ATTRIBUTE_VYSPER_SESSIONSTATEHOLDER = "vysperSessionStateHolder";

    final Logger logger = LoggerFactory.getLogger(XmppIoHandlerAdapter.class);

    protected ServerRuntimeContextProvider contextProvider;
    
	public XmppIoHandlerAdapter(ServerRuntimeContextProvider contextProvider) {
		this.contextProvider=contextProvider;
	}

	/**
	 * Handle unset session context properly. If session state is unconnected, retrieve the domain and create a proper context.
	 * 
	 */
    public void messageReceived(IoSession ioSession, Object message) throws Exception {
    	if(ioSession.getAttribute(ATTRIBUTE_VYSPER_SESSION)==null) {
            SessionStateHolder stateHolder = (SessionStateHolder)ioSession.getAttribute(ATTRIBUTE_VYSPER_SESSIONSTATEHOLDER);
            if(stateHolder==null||stateHolder.getState()!=SessionState.UNCONNECTED) {
            	throw new IllegalStateException("session not properly initialized. close stream");
            }
            SessionContext sessionContext=null;
            if(message instanceof Stanza) {
            	sessionContext=createContext(stateHolder, ioSession, ((Stanza)message).getTo().getDomain());
            }
            if(sessionContext!=null) {
            	ioSession.setAttribute(ATTRIBUTE_VYSPER_SESSION, sessionContext);
            } else {
            	Stanza errorStanza=ServerErrorResponses.getStreamError(StreamErrorCondition.HOST_UNKNOWN, "en", "host not found",null);
                ioSession.write(new StanzaWriteInfo(errorStanza,true));
            	ioSession.getCloseFuture().setClosed();
                return;
            }
    	}
        if (!(message instanceof Stanza)) {
            if (message instanceof XMLText) {
                String text = ((XMLText) message).getText();
                // tolerate reasonable amount of whitespaces for stanza separation
                if (text.length() < 40 && text.trim().length() == 0)
                    return;
            }
            
            messageReceivedNoStanza(ioSession, message);
            return;
        }

        Stanza stanza = (Stanza) message;
        SessionContext session = extractSession(ioSession);
        SessionStateHolder stateHolder = (SessionStateHolder) ioSession
                .getAttribute(ATTRIBUTE_VYSPER_SESSIONSTATEHOLDER);

        session.getServerRuntimeContext().getStanzaProcessor().processStanza(session.getServerRuntimeContext(), session, stanza, stateHolder);
    }
    
    /**
     * Suitable for "normal" XMPP domains.
     * 
     * @param stateHolder the session state holder
     * @param ioSession the io session 
     * @param domain the domain we request a session context for
     * @return the session context for the domain
     */
    protected SessionContext createContext(SessionStateHolder stateHolder,IoSession ioSession,String domain) {
    	ServerRuntimeContext serverRuntimeContext=contextProvider.resolveHostContext(new EntityImpl(null,domain,null));
    	if(serverRuntimeContext.isXmppDomain()) {
            return new MinaBackedSessionContext(serverRuntimeContext, stateHolder, ioSession);
    	}
    	return null;
    }

    private void messageReceivedNoStanza(IoSession ioSession, Object message) {
        if (message == SslFilter.SESSION_SECURED) {
            SessionContext session = extractSession(ioSession);
            SessionStateHolder stateHolder = (SessionStateHolder) ioSession
                    .getAttribute(ATTRIBUTE_VYSPER_SESSIONSTATEHOLDER);
            session.getServerRuntimeContext().getStanzaProcessor().processTLSEstablished(session, stateHolder);
            return;
        } else if (message == SslFilter.SESSION_UNSECURED) {
            // TODO
            return;
            //            throw new IllegalStateException("server must close session!");
        }

        throw new IllegalArgumentException("xmpp handler only accepts Stanza-typed messages, but received type "
                + message.getClass());
    }

    private SessionContext extractSession(IoSession ioSession) {
        return (SessionContext) ioSession.getAttribute(ATTRIBUTE_VYSPER_SESSION);
    }

    public void messageSent(IoSession ioSession, Object o) throws Exception {
        // TODO implement
    }

    public void sessionCreated(IoSession ioSession) throws Exception {
        SessionStateHolder stateHolder = new SessionStateHolder();
        ioSession.setAttribute(ATTRIBUTE_VYSPER_SESSIONSTATEHOLDER, stateHolder);
    }

    public void sessionOpened(IoSession ioSession) throws Exception {
        logger.info("new session from {} has been opened", ioSession.getRemoteAddress());
    }

    public void sessionClosed(IoSession ioSession) throws Exception {
        SessionContext sessionContext = extractSession(ioSession);
        String sessionId = "UNKNOWN";
        if (sessionContext != null) {
            sessionId = sessionContext.getSessionId();
            sessionContext.endSession(SessionContext.SessionTerminationCause.CONNECTION_ABORT);
        }
        logger.info("session {} has been closed", sessionId);
    }

    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {
        logger.debug("session {} is idle", ((SessionContext) ioSession.getAttribute(ATTRIBUTE_VYSPER_SESSION))
                .getSessionId());
    }

    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
        SessionContext sessionContext = extractSession(ioSession);

        Stanza errorStanza;
        if(throwable.getCause() != null && throwable.getCause() instanceof SAXParseException) {
            logger.info("Client sent not well-formed XML, closing session: {}", throwable);
            errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.XML_NOT_WELL_FORMED,
                    sessionContext.getXMLLang(), "Stanza not well-formed", null);
        } else if(throwable instanceof WriteToClosedSessionException) {
            // ignore
            return;
        } else {
            logger.warn("error caught on transportation layer: {}", throwable);
            errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.UNDEFINED_CONDITION,
                    sessionContext.getXMLLang(), "Unknown error", null);

        }
        sessionContext.getResponseWriter().write(errorStanza);
        sessionContext.endSession(SessionContext.SessionTerminationCause.STREAM_ERROR);
    }
}
