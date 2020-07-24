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
package org.apache.vysper.xmpp.modules.core.base.handler;

import java.util.Hashtable;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IQ implementation with default handling for get/set/error/result stanza types
 * this is the recommended superclass for own handler implementations
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class DefaultIQHandler extends IQHandler {
	private static Map<String,StanzaHandler> relayedBack=new Hashtable<String, StanzaHandler>();


    final Logger logger = LoggerFactory.getLogger(DefaultIQHandler.class);

    @Override
    public boolean verify(Stanza stanza) {
        return super.verify(stanza) && verifyInnerElement(stanza);
    }

    protected boolean verifyInnerElement(Stanza stanza) {
        return true;
    }

    protected boolean verifyInnerElementWorker(Stanza stanza, String firstInnerElement) {
        return stanza != null && stanza.getVerifier().subElementsPresentExact(1)
                && stanza.getVerifier().subElementPresent(firstInnerElement);
    }
    
    public static Entity from(Stanza stanza,SessionContext context) {
    	Entity from=stanza.getFrom();
    	return from==null?context.getInitiatingEntity():from;
    }

    @Override
    protected abstract boolean verifyNamespace(Stanza stanza);

    @Override
    protected Stanza executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
            SessionContext sessionContext) {
    	if(stanza.getTo()!=null) {
    		serverRuntimeContext=serverRuntimeContext.resolveDomainContext(stanza.getTo());
    	}
    	if(outboundStanza) {
	        switch (stanza.getIQType()) {
	        case ERROR:
	            handleError(stanza, serverRuntimeContext, sessionContext);
	            return null;
	        case GET:
	            return handleGet(stanza, serverRuntimeContext, sessionContext);
	        case RESULT:
	            return handleResult(stanza,serverRuntimeContext, sessionContext);
	        case SET:
	            return handleSet(stanza, serverRuntimeContext, sessionContext);
	        default:
	            throw new RuntimeException("iq stanza type not supported: " + stanza.getIQType().value());
	        }
    	} else if(sessionContext!=null) {
    		sessionContext.getResponseWriter().write(stanza);
    		return null;
        } else {
            throw new IllegalStateException("handling offline messages not implemented");
    	}
    }

    protected Stanza handleResult(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
    	try {
    		StanzaHandler handler=null;
    		synchronized (relayedBack) {
				//System.out.println("IQ["+stanza.getID()+"] Get backed handler from "+relayedBack);
        		handler=relayedBack.remove(stanza.getID());
			}
    		if(handler!=null) {
    			ResponseStanzaContainer container=handler.execute(stanza, serverRuntimeContext, true, sessionContext, null);
    			if(container!=null) {
    				if(container.hasResponse()) {
    					return container.getResponseStanza();
    				}
    			}
				return null;
    		} else {
    			sessionContext.relay(stanza);
    			return null;
    		}
    	} catch(Throwable t) {
    		logger.error("Relaying iq result failed.",t);
    	}
		return null;
//        logger.warn("IQ 'result' stanza not handled by {}: {}", getClass().getCanonicalName(), DenseStanzaLogRenderer
//                .render(stanza));
//        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
//                StanzaErrorType.CANCEL, "iq stanza of type 'result' is not handled for this namespace",
//                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }

    protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        logger.warn("IQ 'get' stanza not handled by {}: {}", getClass().getCanonicalName(), DenseStanzaLogRenderer
                .render(stanza));
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
                StanzaErrorType.CANCEL, "iq stanza of type 'get' is not handled for this namespace",
                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }

    protected void handleError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
    	if(!serverRuntimeContext.relay(stanza)) {
    		logger.error("Relaying iq result failed.");
    	}
//
//        logger.warn("IQ 'error' stanza not handled by {}: {}", getClass().getCanonicalName(), DenseStanzaLogRenderer
//                .render(stanza));
//        throw new RuntimeException("iq stanza type ERROR not yet handled");
    }

    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
    	//serverRuntimeContext.getComponentStanzaProcessor(stanza.getTo()).processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);
    	System.out.println("\nTODO IQ #id"+stanza.getID()+" type set from "+stanza.getFrom()+" to "+stanza.getTo());
        logger.warn("IQ 'set' stanza not handled by {}: {}", getClass().getCanonicalName(), DenseStanzaLogRenderer
                .render(stanza));
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
                StanzaErrorType.CANCEL, "iq stanza of type 'set' is not handled for this namespace",
                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }

	public static void registerResultHandler(Stanza stanza, StanzaHandler handler) {
		registerResultHandler(stanza.getAttributeValue("id"),handler);
	}
	
	public static void registerResultHandler(String id, StanzaHandler handler) {
		if(id!=null) {
			synchronized(relayedBack) {
				relayedBack.put(id, handler);
			}
		}		
	}
}
