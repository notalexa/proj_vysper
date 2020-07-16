package org.apache.vysper.xmpp.extension.xep0114;

import static org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

public class ExternalComponentModule implements Module {
	ServerRuntimeContext runtimeContext;
	List<ExternalComponent> components=new ArrayList<ExternalComponent>();

	@Override
	public List<HandlerDictionary> getHandlerDictionaries() {
		return Collections.emptyList();
	}
	
	public ExternalComponentModule addComponent(ExternalComponent component) {
		components.add(component);
		return this;
	}

	@Override
	public String getName() {
		return "XEP-0114 Jabber Component Protocol";
	}

	@Override
	public List<ServerRuntimeContextService> getServerServices() {
		return Collections.emptyList();
	}

	@Override
	public String getVersion() {
		return "1.6";
	}

	@Override
	public void initialize(ServerRuntimeContext runtimeContext) {
		this.runtimeContext=runtimeContext;
		for(ExternalComponent externalComponent:components) {
			externalComponent.setServerRuntimeContext(runtimeContext);
		}
        ServerRuntimeContextService service = runtimeContext
                .getServerRuntimeContextService(SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
        if (service != null && service instanceof ServiceDiscoveryRequestListenerRegistry) {
            ServiceDiscoveryRequestListenerRegistry requestListenerRegistry = (ServiceDiscoveryRequestListenerRegistry) service;
            for (ItemRequestListener itemRequestListener : components) {
                if (itemRequestListener != null) {
                	requestListenerRegistry.addItemRequestListener(itemRequestListener);
                }
            }
        } else {
            System.out.println("cannot register disco request listeners: Registry service not found.");
        }
	}
	
	public ExternalComponent get(String subdomain) {
		for(ExternalComponent component:components) {
			if(component.getSubdomain().equals(subdomain)) {
				return component;
			}
		}
		return null;
	}
	
	public Endpoint createEndPoint(int port) {
		return new ExternalComponentEndpoint(port);
	}
}
