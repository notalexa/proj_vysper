package org.apache.vysper.xmpp.server;

import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authorization.UserAuthorization;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.presence.LatestPresenceCache;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

public class ServerRuntimeContextAdapter implements ServerRuntimeContext {
	protected ServerRuntimeContext delegate;
	public ServerRuntimeContextAdapter(ServerRuntimeContext delegate) {
		this.delegate=delegate;
	}

	@Override
	public StanzaHandler getHandler(Stanza stanza) {
		return delegate.getHandler(stanza);
	}
	
	@Override
	public String getNextSessionId() {
		return delegate.getNextSessionId();
	}
	
	@Override
	public Entity getServerEnitity() {
		return delegate.getServerEnitity();
	}
	
	@Override
	public String getDefaultXMLLang() {
		return delegate.getDefaultXMLLang();
	}
	
	@Override
	public StanzaProcessor getStanzaProcessor() {
		return delegate.getStanzaProcessor();
	}
	
	@Override
	public StanzaRelay getStanzaRelay() {
		return delegate.getStanzaRelay();
	}
	
	@Override
	public ServerFeatures getServerFeatures() {
		return delegate.getServerFeatures();
	}
	
	@Override
	public SSLContext getSslContext() {
		return delegate.getSslContext();
	}
	
	@Override
	public UserAuthorization getUserAuthorization() {
		return delegate.getUserAuthorization();
	}
	
	@Override
	public ResourceRegistry getResourceRegistry() {
		return delegate.getResourceRegistry();
	}
	
	@Override
	public LatestPresenceCache getPresenceCache() {
		return delegate.getPresenceCache();
	}
	
	@Override
	public void registerServerRuntimeContextService(ServerRuntimeContextService service) {
		delegate.registerServerRuntimeContextService(service);
	}
	
	@Override
	public ServerRuntimeContextService getServerRuntimeContextService(String name) {
		return delegate.getServerRuntimeContextService(name);
	}
	
	@Override
	public StorageProvider getStorageProvider(Class<? extends StorageProvider> clazz) {
		return delegate.getStorageProvider(clazz);
	}
	
	@Override
	public void registerComponent(Component component) {
		delegate.registerComponent(component);
	}
	
	@Override
	public StanzaProcessor getComponentStanzaProcessor(Entity entity) {
		return delegate.getComponentStanzaProcessor(entity);
	}
	
	@Override
	public XMPPServerConnectorRegistry getServerConnectorRegistry() {
		return delegate.getServerConnectorRegistry();
	}
	
	@Override
	public List<Module> getModules() {
		return delegate.getModules();
	}
	
	@Override
	public <T> T getModule(Class<T> clazz) {
		return delegate.getModule(clazz);
	}
	
	@Override
	public boolean isXmppDomain() {
		return delegate.isXmppDomain();
	}
	
	@Override
	public ServerRuntimeContext resolveDomainContext(Entity entity) {
		return delegate.resolveDomainContext(entity);
	}

	public Entity getFrom(SessionContext sessionContext, Stanza stanza) {
		return delegate.getFrom(sessionContext, stanza);
	}

	public ServerRuntimeContext getDomainContext() {
		return delegate.getDomainContext();
	}
}
