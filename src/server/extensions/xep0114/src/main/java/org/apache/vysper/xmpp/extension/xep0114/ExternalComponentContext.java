package org.apache.vysper.xmpp.extension.xep0114;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.extension.xep0114.handler.HandshakeHandler;
import org.apache.vysper.xmpp.extension.xep0114.handler.StreamStartHandler;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContextAdapter;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

public class ExternalComponentContext extends ServerRuntimeContextAdapter {
	protected ExternalComponent component;
	protected Entity componentEntity;
	protected XEP0114Handlers handlers;
	public ExternalComponentContext(ExternalComponent component,ServerRuntimeContext hostContext) {
		super(hostContext);
		this.component=component;
		this.componentEntity=new EntityImpl(null, component.getSubdomain()+"."+hostContext.getServerEnitity().getDomain(),null);
		this.handlers=new XEP0114Handlers();
	}

	@Override
	public StanzaProcessor getStanzaProcessor() {
		return component.getStanzaProcessor();
	}

	@Override
	public StanzaHandler getHandler(Stanza stanza) {
		return handlers.get(stanza);
	}

	@Override
	public Entity getServerEnitity() {
		return componentEntity;
	}
	
	public class XEP0114Handlers {
		Map<String,StanzaHandler> handlers=new HashMap<>();
		public XEP0114Handlers() {
			handlers.put("stream",new StreamStartHandler());
			handlers.put("handshake",new HandshakeHandler(component));
		}
		
		public StanzaHandler get(Stanza stanza) {
			StanzaHandler handler=handlers.get(stanza.getName());
			if(handler!=null) {
				return handler;
			} else {
				return component.getComponentHandler();
			}
		}
	}
	
	@Override
	public boolean isXmppDomain() {
		return false;
	}

	public SessionContext createSession(SessionStateHolder stateHolder, IoSession ioSession, Entity entity) {
		return new ExternalComponentSessionContext(component, stateHolder, ioSession);
	}

	@Override
	public boolean relay(Stanza stanza, DeliveryFailureStrategy failureStrategy) {
		return component.relay(stanza, failureStrategy);
	}
}
