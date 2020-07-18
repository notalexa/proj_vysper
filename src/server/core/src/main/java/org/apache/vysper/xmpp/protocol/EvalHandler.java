package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * This handler evaluates the original handler on a different runtime context
 * (the context of the "to" attribute in general).
 * 
 * @author notalexa
 *
 */
public class EvalHandler implements StanzaHandler {
	protected StanzaHandler handler;
	protected ServerRuntimeContext evalContext;
	
	public EvalHandler(ServerRuntimeContext evalContext,StanzaHandler handler) {
		this.evalContext=evalContext;
		this.handler=handler;
	}
	
	public String getName() {
		return handler.getName();
	}
	
	public boolean verify(Stanza stanza) {
		return handler.verify(stanza);
	}
	
	public boolean isSessionRequired() {
		return handler.isSessionRequired();
	}
	
	public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) throws ProtocolException {
		return handler.execute(stanza, evalContext, isOutboundStanza, sessionContext, sessionStateHolder);
	}
}
