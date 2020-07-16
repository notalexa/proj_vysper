package org.apache.vysper.xmpp.extension.xep0114;

import org.apache.mina.core.session.IoSession;
import org.apache.vysper.mina.MinaBackedSessionContext;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;

public class ExternalComponentSessionContext extends MinaBackedSessionContext {
	public static final String JABBER_COMPONENT_ACCEPT="jabber:component:accept";
	
	public ExternalComponentSessionContext(ExternalComponent component, SessionStateHolder sessionStateHolder, IoSession minaSession) {
		super(component.getComponentContext(), sessionStateHolder, minaSession);
		setInitiatingEntity(serverRuntimeContext.getServerEnitity());
	}
	
	public String getSessionNamespaceURI() {
		return JABBER_COMPONENT_ACCEPT;
	}
}
