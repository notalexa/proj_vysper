package org.apache.vysper.xmpp.extension.xep0114;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.extension.xep0114.handler.ProtocolWorker;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

public class ExternalComponent implements Component, ItemRequestListener {
	protected String subdomain;
	protected String password;
	protected ServerRuntimeContext componentContext;
	protected ServerRuntimeContext hostContext;
	protected StanzaProcessor processor;
	protected StanzaHandler relayHandler=new StanzaHandler() {

		@Override
		public String getName() {
			return "relay to "+getServiceDomain();
		}

		@Override
		public boolean verify(Stanza stanza) {
			return componentContext.getServerEnitity().equals(stanza.getTo());
		}

		@Override
		public boolean isSessionRequired() {
			return true;
		}

		@Override
		public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) throws ProtocolException {
			if(sessionStateHolder.getState()!=SessionState.AUTHENTICATED||!sessionContext.relay(stanza)) {
				Stanza errorStanza=ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNEXPECTED_REQUEST, XMPPCoreStanza.getWrapper(stanza),
	            	StanzaErrorType.CANCEL,
	                "unable to relay",
	                sessionContext.getXMLLang(), null);
				return new ResponseStanzaContainerImpl(errorStanza);
			}
			return null;
		}
	};

	public ExternalComponent(String subdomain,String password) {
		this.subdomain=subdomain;
		this.password=password;
		processor=new ProtocolWorker(this);
	}
	
	public void setServerRuntimeContext(ServerRuntimeContext context) {
		this.hostContext=context;
		this.componentContext=new ExternalComponentContext(this,context);
		context.registerComponent(this);
	}
	
	@Override
	public String getSubdomain() {
		return subdomain;
	}
	
	public String getServiceDomain() {
		return componentContext.getServerEnitity().getDomain();
	}
	
	public String getPassword() {
		return password;
	}

	@Override
	public StanzaProcessor getStanzaProcessor() {
		return processor;
	}
	
	
	public StanzaHandler getComponentHandler() {
		return relayHandler;
	}

	@Override
	public List<Item> getItemsFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        if (hostContext.getServerEnitity().equals(to)) {
               return Collections.singletonList(new Item(componentContext.getServerEnitity()));
        }
		return null;
	}

	@Override
	public ServerRuntimeContext getComponentContext() {
		return componentContext;
	}

	public boolean relay(Stanza stanza, DeliveryFailureStrategy failureStrategy) {
		for(SessionContext targetSession:componentContext.getResourceRegistry().getSessions(stanza.getTo())) {
			targetSession.getResponseWriter().write(stanza);
		}
		return true;
	}
}
