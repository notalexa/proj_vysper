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

package org.apache.vysper.xmpp.server;

import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.IoSession;
import org.apache.vysper.mina.MinaBackedSessionContext;
import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.UserAuthorization;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.presence.LatestPresenceCache;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 * provides each session with server-global data
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface ServerRuntimeContext {
    StanzaHandler getHandler(Stanza stanza);

    String getNextSessionId();

    Entity getServerEnitity();

    String getDefaultXMLLang();

    StanzaProcessor getStanzaProcessor();

    StanzaRelay getStanzaRelay();

    ServerFeatures getServerFeatures();

    SSLContext getSslContext();

    UserAuthorization getUserAuthorization();

    ResourceRegistry getResourceRegistry();

    LatestPresenceCache getPresenceCache();

    void registerServerRuntimeContextService(ServerRuntimeContextService service);

    ServerRuntimeContextService getServerRuntimeContextService(String name);

    StorageProvider getStorageProvider(Class<? extends StorageProvider> clazz);

    void registerComponent(Component component);

    StanzaProcessor getComponentStanzaProcessor(Entity entity);
    
    XMPPServerConnectorRegistry getServerConnectorRegistry();
    
    List<Module> getModules();

    <T> T getModule(Class<T> clazz);

    /**
     * Resolve the domain context for a given entity
     * 
     * @param entity the entity we need the server context for
     * @return the corresponding context
     */
    default ServerRuntimeContext resolveDomainContext(Entity entity) {
    	return this;
    }
    
    /**
     * 
     * @return the context of the domain (in general different if this is
     * a context of a component)
     */
    default ServerRuntimeContext getDomainContext() {
    	return this;
    }
    
    /**
     * 
     * @return <code>true</code> if this is not a component context.
     */
    default boolean isXmppDomain() {
    	return true;
    }

	default Entity getFrom(SessionContext sessionContext, Stanza stanza,boolean includeResource) {
		Entity from=stanza.getFrom();
		if(from==null) {
			from=sessionContext.getInitiatingEntity();
		}
		if(includeResource&&from!=null&&from.getResource()==null) {
			List<String> bound=getResourceRegistry().getResourcesForSession(sessionContext);
			if(bound.size()==1) {
				from=new EntityImpl(from,bound.get(0));
			}
		}
		return from;
	}
	
	/**
	 * Relay handling using an failure ignore strategy.
	 * 
	 * @param stanza the stanza to relay
	 * @return <code>true<code>if relaying was successful (with respect to the failure strategy)
	 */
	default boolean relay(Stanza stanza) {
		return relay(stanza,DeliveryFailureStrategy.IGNORE);
	}
	
	/**
	 * 
	 * @param stanza the stanza to relay
	 * @param failureStrategy the failure strategy
	 * @return <code>true<code>if relaying was successful (with respect to the failure strategy)
	 */
	default boolean relay(Stanza stanza,DeliveryFailureStrategy failureStrategy) {
		try {
			resolveDomainContext(stanza.getTo()).getStanzaRelay().relay(stanza.getTo(),stanza,failureStrategy);
			return true;
		} catch(DeliveryException e) {
			return false;
		}
	}
	
	public class ComponentContext extends ServerRuntimeContextAdapter {
		protected Entity componentEntity;
		public ComponentContext(Entity componentJID,ServerRuntimeContext serverContext) {
			super(serverContext);
			this.componentEntity=componentJID;
		}
		@Override
		public Entity getServerEnitity() {
			return componentEntity;
		}
	}

	default SessionContext createSession(SessionStateHolder stateHolder, IoSession ioSession, Entity entity) {
		MinaBackedSessionContext context=new MinaBackedSessionContext(this, stateHolder, ioSession);
		context.setInitiatingEntity(entity);
		return context;
	}
}
