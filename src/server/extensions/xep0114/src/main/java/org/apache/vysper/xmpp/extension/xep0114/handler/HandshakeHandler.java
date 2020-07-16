package org.apache.vysper.xmpp.extension.xep0114.handler;

import org.apache.vysper.xmpp.extension.xep0114.ExternalComponent;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

public class HandshakeHandler implements StanzaHandler {
	protected ExternalComponent component;
	public HandshakeHandler(ExternalComponent component) {
		this.component=component;
	}

	@Override
	public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext context, boolean outbound, SessionContext sessionContext, SessionStateHolder state) throws ProtocolException {
		// TODO check for the correct password
		state.setState(SessionState.AUTHENTICATED);
		// TODO should be bound to the external component
		sessionContext.bindResource();
		return new ResponseStanzaContainerImpl(new StanzaBuilder("handshake").build());
	}

	@Override
	public String getName() {
		return "handshake";
	}

	@Override
	public boolean isSessionRequired() {
		return true;
	}

	@Override
	public boolean verify(Stanza arg0) {
		return true;
	}
}
