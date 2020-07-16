package org.apache.vysper.xmpp.server;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * A runtime context suitable for remote XMPP servers. Currently, this context produces an
 * error effectively prohibiting all server to server traffic.
 * 
 * @author notalexa
 *
 */
public class RemoteServerRuntimeContext extends ServerRuntimeContextAdapter implements StanzaHandler {
	private Entity serverEntity;
	private StanzaRelay relay=new NotRelayingRelay();
	public RemoteServerRuntimeContext(ServerRuntimeContext delegate, Entity serverEntity) {
		super(delegate);
		this.serverEntity=serverEntity;
	}
	@Override
	public StanzaHandler getHandler(Stanza stanza) {
		return this;
	}
	
	@Override
	public Entity getServerEnitity() {
		return serverEntity;
	}
	@Override
	public ServerFeatures getServerFeatures() {
		return super.getServerFeatures();
	}
	
	@Override
	public List<Module> getModules() {
		return Collections.emptyList();
	}
	@Override
	public <T> T getModule(Class<T> clazz) {
		return null;
	}
	@Override
	public String getName() {
		return "relay to "+serverEntity.getDomain();
	}
	@Override
	public boolean verify(Stanza stanza) {
		return true;
	}
	@Override
	public boolean isSessionRequired() {
		return true;
	}
	@Override
	public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) throws ProtocolException {
		Stanza errorStanza=ServerErrorResponses.getStanzaError(StanzaErrorCondition.REMOTE_SERVER_NOT_FOUND, XMPPCoreStanza.getWrapper(stanza), StanzaErrorType.CONTINUE, "", getDefaultXMLLang(), null);
		return new ResponseStanzaContainerImpl(errorStanza);
	}
	@Override
	public boolean isXmppDomain() {
		return false;
	}
	@Override
	public StanzaRelay getStanzaRelay() {
		return relay;
	}
	
	public class NotRelayingRelay implements StanzaRelay {
		@Override
		public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException {
			throw new DeliveryException("Not allowed to relay to "+getServerEnitity()+" delivering to "+receiver);
		}		
	}
}
