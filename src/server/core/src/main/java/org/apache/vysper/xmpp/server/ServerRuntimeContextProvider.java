package org.apache.vysper.xmpp.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.Module;

/**
 * Provides context for a given domain. If the context is not known, a {@link RemoteServerRuntimeContext} is created for this domain.
 * 
 * @author notalexa
 */
public class ServerRuntimeContextProvider {
	// The first registered context is used for remote runtime contexts.
	private ServerRuntimeContext relayContext;
	
	protected Map<String,ServerRuntimeContext> defined=new HashMap<>();
	protected Map<String,ServerRuntimeContext> resolved=new HashMap<>();
	
	public ServerRuntimeContextProvider() {
	}
	
	public ServerRuntimeContextProvider(ServerRuntimeContext... contexts) {
		for(ServerRuntimeContext context:contexts) {
			add(context);
		}
	}
	
	public ServerRuntimeContextProvider add(ServerRuntimeContext context) {
		if(relayContext==null) {
			relayContext=context;
		}
		defined.put(context.getServerEnitity().getDomain(),context);
		resolved.put(context.getServerEnitity().getDomain(),context);
		return this;
	}
	
	public <T extends Module> T getModule(String componentName,Class<T> clazz) {
		for(Map.Entry<String,ServerRuntimeContext> context:defined.entrySet()) {
			if(componentName.endsWith(context.getKey())) {
				T module=context.getValue().getModule(clazz);
				if(module!=null&&componentName.equals(module.getName()+"."+context.getKey())) {
					return module;
				}
			}
		}
		return null;
	}
	
	/**
	 * Resolve a context for the domain of the given entity.
	 * 
	 * @param entity the domain
	 * @return a context for the domain of the entity (never <code>null<code>)
	 */
	public synchronized ServerRuntimeContext resolveDomainContext(Entity entity) {
		return resolveDomainContext(entity.getDomain());
	}
	
	/**
	 * Resolve a context for the domain.
	 * 
	 * @param domain the domain
	 * @return a context for the domain (never <code>null<code>)
	 */
	public synchronized ServerRuntimeContext resolveDomainContext(String domain) {
		ServerRuntimeContext context=resolved.get(domain);
		if(context!=null) {
			return context;
		}
		context=defined.get(domain);
		if(context!=null) {
			resolved.put(domain,context);
			return context;
		}
		int p=domain.indexOf('.');
		if(p>0) {
			context=defined.get(domain.substring(p+1));
			if(context!=null) {
				resolved.put(domain,context);
				return context;
			}
		}
		// Not found. We should handle remote context later.
		context=new RemoteServerRuntimeContext(relayContext,new EntityImpl(null,domain,null));
		resolved.put(domain,context);
		return context;
	}
}
