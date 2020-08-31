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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCIqAdminHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCIqOwnerHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCIqResultHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCIqSetHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCMessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.MUCPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.ComponentStanzaHandlerLookup;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public final class MUCModule extends DefaultDiscoAwareModule implements Component, ComponentInfoRequestListener,
        ItemRequestListener {

    private String subdomain = "chat";
    private StorageProviderRegistry registry;

    private Conference conference;

    private Entity fullDomain;

    private final Logger logger = LoggerFactory.getLogger(MUCModule.class);

    private ServerRuntimeContext serverRuntimeContext;
    private ServerRuntimeContext componentRuntimeContext;

    private ComponentStanzaProcessor stanzaProcessor;

    public MUCModule(StorageProviderRegistry registry,String subdomain) {
        this(registry,subdomain, new Conference("Conference"));
    }

    public MUCModule(StorageProviderRegistry registry) {
    	this(registry,"conference");
    }

    public MUCModule(StorageProviderRegistry registry,String subdomain, Conference conference) {
        this.subdomain = subdomain;
        this.conference = conference;
        this.registry=registry;
        conference.initialize(this,registry);
    }
    
    public Conference getConference() {
    	return conference;
    }
    
    public Entity getRoomJid(Room room) {
    	return new EntityImpl(room.getNodeName(),fullDomain.getDomain(),null);
    }
    
    public Room createRoom(String nodeName,String descr,RoomType...types) {
    	return conference.createRoom(nodeName, descr, types);
    }

    /**
     * Initializes the MUC module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);
        this.serverRuntimeContext = serverRuntimeContext;
        fullDomain = EntityUtils.createComponentDomain(subdomain, serverRuntimeContext);
        ComponentStanzaHandlerLookup lookup=new ComponentStanzaHandlerLookup();
        componentRuntimeContext=new ServerRuntimeContext.ComponentContext(new EntityImpl(null,subdomain+"."+serverRuntimeContext.getServerEnitity().getDomain(),null), serverRuntimeContext) {
			@Override
			public StanzaHandler getHandler(Stanza stanza) {
				StanzaHandler handler=lookup.getHandler(stanza);
				if(handler!=null) {
					return handler;
				}
				return super.getHandler(stanza);
			}

			@Override
			public boolean relay(Stanza stanza) {
				return relay(stanza,new DeliveryFailureStrategy() {
					
					@Override
					public void process(StanzaRelay relay, Stanza failedToDeliverStanza, List<DeliveryException> deliveryException) throws DeliveryException {
						try {
							Room room=getConference().findRoom(failedToDeliverStanza.getFrom().getNode());
							if(room!=null) {
								Occupant occupant=room.findOccupantByNick(failedToDeliverStanza.getFrom().getResource());
								if(occupant!=null) {
									// Kick out
									occupant.leaveAsync("unreachable");
								}
							}
						} catch(Throwable t) {}
					}
				});
			}
        };
        lookup.addDefaultHandler(new MUCPresenceHandler(this));
        lookup.addDefaultHandler(new MUCMessageHandler(conference, fullDomain));

        ComponentStanzaProcessor processor = new ComponentStanzaProcessor(serverRuntimeContext);
        processor.addHandler(new MUCPresenceHandler(this));
        processor.addHandler(new MUCMessageHandler(conference, fullDomain));
        processor.addHandler(new MUCIqAdminHandler(conference));
        processor.addHandler(new MUCIqOwnerHandler(conference));
        processor.addHandler(new MUCIqSetRelay());
        stanzaProcessor = processor;

//        RoomStorageProvider roomStorageProvider = (RoomStorageProvider) serverRuntimeContext
//                .getStorageProvider(RoomStorageProvider.class);
//        OccupantStorageProvider occupantStorageProvider = (OccupantStorageProvider) serverRuntimeContext
//                .getStorageProvider(OccupantStorageProvider.class);
//
//        if (roomStorageProvider == null) {
//            logger.warn("No room storage provider found, using the default (in memory)");
//        } else {
//            conference.setRoomStorageProvider(roomStorageProvider);
//        }
//        if (occupantStorageProvider == null) {
//            logger.warn("No occupant storage provider found, using the default (in memory)");
//        } else {
//            conference.setOccupantStorageProvider(occupantStorageProvider);
//        }
        serverRuntimeContext.getResourceRegistry().addBindListener(conference.getRoomStorageProvider());

//        if (occupantStorageProvider == null) {
//            logger.warn("No occupant storage provider found, using the default (in memory)");
//        } else {
//            conference.setOccupantStorageProvider(occupantStorageProvider);
//        }
//        conference.initialize(this);
        serverRuntimeContext.registerComponent(this);
    }

    @Override
    public String getName() {
        return "XEP-0045 Multi-user chat";
    }

    @Override
    public String getVersion() {
        return "1.33.0";
    }

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }

    public List<InfoElement> getComponentInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        if (!fullDomain.getDomain().equals(request.getTo().getDomain()))
            return null;

        if (request.getTo().getNode() == null) {
            List<InfoElement> serverInfos = conference.getServerInfosFor(request);
            return serverInfos;
        } else {
            // might be an items request on a room
            Room room = conference.findRoom(request.getTo().getNode());
            if (room == null)
                return null;

            if (request.getTo().getResource() != null) {
                // request for an occupant
                Occupant occupant = room.findOccupantByNick(request.getTo().getResource());
                // request for occupant, relay
                if (occupant != null) {
                	request.setRelayed();
                	DefaultIQHandler.registerResultHandler(request.getID(), new MUCIqResultHandler(conference,occupant));
                    relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
                }
                return null;
            } else {
                return room.getInfosFor(request);
            }
        }
    }

    @Override
    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        componentInfoRequestListeners.add(this);
    }

    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener} interface.
     * Makes this modules available via disco#items and returns the associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest)
     */
    public List<Item> getItemsFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        Entity to = request.getTo();
        if (to.getNode() == null) {
            // react on request send to server domain or this subdomain, but not to others
            if (fullDomain.equals(to)) {
                List<Item> conferenceItems = conference.getItemsFor(request);
                return conferenceItems;
            } else if (serverRuntimeContext.getServerEnitity().equals(to)) {
                List<Item> componentItem = new ArrayList<Item>();
                componentItem.add(new Item(fullDomain));
                return componentItem;
            }
            return null;
        } else if (fullDomain.getDomain().equals(to.getDomain())) {
            // might be an items request on a room
            Room room = conference.findRoom(to.getNode());
            if (room != null) {
                if (to.getResource() != null) {
                    // request for an occupant
                    Occupant occupant = room.findOccupantByNick(to.getResource());
                    // request for occupant, relay
                    if (occupant != null) {
                        relayDiscoStanza(occupant.getJid(), request, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
                    }
                } else {
                    return room.getItemsFor(request);
                }
            }
        }
        return null;
    }

    private void relayDiscoStanza(Entity receiver, InfoRequest request, String ns) {
        StanzaBuilder builder = StanzaBuilder.createIQStanza(request.getFrom(), receiver, IQStanzaType.GET, request
                .getID());
        builder.startInnerElement("query", ns);
        if (request.getNode() != null) {
            builder.addAttribute("node", request.getNode());
        }
        serverRuntimeContext.relay(builder.build());
    }

    public String getSubdomain() {
        return subdomain;
    }

    public StanzaProcessor getStanzaProcessor() {
        return stanzaProcessor;
    }

	@Override
	public List<HandlerDictionary> getHandlerDictionaries() {
		return Collections.singletonList(new HandlerDictionary() {
			
			@Override
			public void seal() {
			}
			
			@Override
			public void register(StanzaHandler stanzaHandler) {
			}
			
			@Override
			public StanzaHandler get(Stanza stanza) {
				if("iq".equals(stanza.getName())&&stanza.getTo().getDomain().equals(fullDomain.getDomain())) {
					if(NamespaceURIs.XEP0045_MUC_OWNER.equals(stanza.getFirstInnerElement().getNamespaceURI())/*&&stanza.getTo().equals(fullDomain)*/) {
						return new MUCIqOwnerHandler(conference);
					} else if(NamespaceURIs.XEP0045_MUC_ADMIN.equals(stanza.getFirstInnerElement().getNamespaceURI())) {
						return new MUCIqAdminHandler(conference);
					} else if("set".equals(stanza.getAttributeValue("type"))&&stanza.getTo()!=null) {
						return new MUCIqSetHandler(conference);
					}
				}
				return null;
			}
		});
	}
	
	public class MUCIqSetRelay implements StanzaHandler {

		@Override
		public String getName() {
			return "iq rely";
		}

		@Override
		public boolean verify(Stanza stanza) {
			if("iq".equals(stanza.getName())&&"set".equals(stanza.getAttributeValue("type"))&&stanza.getTo()!=null&&stanza.getTo().isNodeSet()&&stanza.getTo().isResourceSet()&&stanza.getTo().getDomain().equals(fullDomain.getDomain())) {
				return true;
			}
			return false;
		}

		@Override
		public boolean isSessionRequired() {
			return false;
		}

		@Override
		public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
				boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder)
				throws ProtocolException {
			if(!isOutboundStanza) {
				Entity to=stanza.getTo();
				Room room=conference.findRoom(to.getNode());
				if(room!=null) {
					Occupant occupant=room.findOccupantByNick(to.getResource());
					if(occupant!=null) {
						for(SessionContext context:serverRuntimeContext.getResourceRegistry().getSessions(occupant.getJid())) {
							DefaultIQHandler.registerResultHandler(stanza,new MUCIqResultHandler(conference, occupant));
							context.getResponseWriter().write(stanza);
						}
					}
				}
			}
			return null;
		}
		
	}

	@Override
	public ServerRuntimeContext getComponentContext() {
		return componentRuntimeContext;
	}
}
