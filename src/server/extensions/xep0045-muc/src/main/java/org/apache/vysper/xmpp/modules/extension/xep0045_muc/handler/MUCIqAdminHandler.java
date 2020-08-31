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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCIqAdminHandler extends DefaultIQHandler {

    final Logger logger = LoggerFactory.getLogger(MUCIqAdminHandler.class);

    private Conference conference;

    public MUCIqAdminHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC_ADMIN);
    }

    private Entity roomAndNick(Room room, Occupant occupant) {
        return new EntityImpl(room.getJID(), occupant.getNick());
    }
    
    public Entity getFrom(SessionContext sessionContext,Stanza stanza) {
        Entity from=stanza.getFrom();
        if(from==null) {
        	from=sessionContext.getInitiatingEntity();
        	if(from.getResource()==null) {
        		List<String> resources=sessionContext.getServerRuntimeContext().getResourceRegistry().getResourcesForSession(sessionContext);
        		if(resources.size()==1) {
        			from=new EntityImpl(from,resources.get(0));
        		}
        	}
        }
        return from;
    }

    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        logger.debug("Received MUC admin stanza");
        
        Room room = conference.findRoom(stanza.getTo().getNode());
        Occupant moderator = room.findOccupantByJID(getFrom(sessionContext,stanza));

        // check if moderator
        if (moderator == null || moderator.getRole() != Role.Moderator) {
            // only moderators are allowed to continue
            logger.debug("Only moderators are allowed to issue admin stanzas ("+moderator+")");
            
            return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.AUTH, StanzaErrorCondition.FORBIDDEN);
        }
        try {
            XMLElement query = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0045_MUC_ADMIN);
            XMLElement itemElement = query.getSingleInnerElementsNamed("item", NamespaceURIs.XEP0045_MUC_ADMIN);

            IqAdminItem item;
            try {
                item = IqAdminItem.getWrapper(itemElement);
            } catch (EntityFormatException e) {
                return createBadRequestError(stanza, serverRuntimeContext, sessionContext,
                    "Invalid JID");
            }

            if (item.getRole() != null) {
                logger.debug("Changing role");
                return changeRole(stanza, serverRuntimeContext, sessionContext, item, room, moderator);
            } else if(item.getAffiliation() != null) {
                logger.debug("Changing affiliation");
                return changeAffiliation(stanza, serverRuntimeContext, sessionContext, item, room, moderator);
            } else {
                logger.debug("Invalid MUC admin stanza");
                return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Unknown IQ stanza");
            }

        } catch (XMLSemanticError e) {
            logger.debug("Invalid MUC admin stanza", e);
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext,
                    "Invalid IQ stanza");
        }

    }

    private Stanza createBadRequestError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, String message) {
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                StanzaErrorType.MODIFY, message,
                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }
    
    private Stanza changeAffiliation(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, IqAdminItem item, Room room, Occupant moderator) {
        // only allowed by admins and owners
        if(moderator.getAffiliation() != Affiliation.Admin && moderator.getAffiliation() != Affiliation.Owner) {
            return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                    StanzaErrorCondition.NOT_ALLOWED);
        }
        
        
        Entity target = null;
        
        if (item.getNick() != null) {
            target = room.findOccupantByNick(item.getNick()).getJid();
        } else {
            try {
                if(item.getJid() != null) {
                    target = item.getJid();
                } else {
                    return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Missing nick for item");
                }
            } catch (EntityFormatException e) {
                return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Invalid JID");
            }
        }
        
        Affiliation currentAffiliation = room.getAffiliation(target);
        Affiliation newAffiliation = item.getAffiliation();

        // if the target is present in the room, we need to send presence updates
        // otherwise we should send messages
        Occupant targetOccupant = room.findOccupantByJID(target);
        
        // notify remaining users that user got affiliation updated
        PresenceStanzaType presenceType = null;
        Status status = null;
        Role newRole;
        Entity from;
        if(targetOccupant != null) {
            newRole = targetOccupant.getRole();
            from = roomAndNick(room, targetOccupant);
        } else {
            newRole = Role.None;
            from = room.getJID();
            
        }
        
        // only owners can revoke ownership and admin
        if((currentAffiliation == Affiliation.Owner || currentAffiliation == Affiliation.Admin) && moderator.getAffiliation() != Affiliation.Owner) {
            return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                    StanzaErrorCondition.NOT_ALLOWED);
        }
        
        // if the occupant is getting revoke as a member, and this is a members-only room, he also needs to leave the room
        if((newAffiliation == Affiliation.None && room.isRoomType(RoomType.MembersOnly)) || newAffiliation == Affiliation.Outcast) {
            if(newAffiliation == Affiliation.Outcast && targetOccupant.getAffiliation().compareTo(moderator.getAffiliation()) < 0) {
                return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
            
            if(targetOccupant != null) {
                room.removeOccupant(target);
            }
            presenceType = PresenceStanzaType.UNAVAILABLE;
            
            if(newAffiliation == Affiliation.Outcast) {
                status = new Status(StatusCode.BEEN_BANNED);
            } else {
                status = new Status(StatusCode.REMOVED_BY_AFFILIATION);
            }

            newRole = Role.None;
            
            MucUserItem presenceItem = new MucUserItem(newAffiliation, newRole);

            Stanza presenceToFormerMember = MUCStanzaBuilder.createPresenceStanza(moderator,from, targetOccupant.getJid(),
                    presenceType, NamespaceURIs.XEP0045_MUC_USER, presenceItem, status);

            relayStanza(targetOccupant.getJid(), presenceToFormerMember, sessionContext);
        } else if(newAffiliation == Affiliation.Owner || newAffiliation == Affiliation.Admin) {
            if(moderator.getAffiliation() != Affiliation.Owner) {
                return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        	newRole=Role.Moderator;
        	if(targetOccupant!=null) {
        		targetOccupant.setRole(newRole);
        	}
        }
        if(targetOccupant!=null) {
        	targetOccupant.setAffiliation(newAffiliation);
        }
            

        if(targetOccupant != null) {
            MucUserItem presenceItem = new MucUserItem(targetOccupant,true,false);
            for (Occupant occupant : room.getOccupants()) {
                Stanza presenceToRemaining = MUCStanzaBuilder.createPresenceStanza(targetOccupant,from, occupant.getJid(),
                        presenceType, NamespaceURIs.XEP0045_MUC_USER, presenceItem, status);
                relayStanza(occupant.getJid(), presenceToRemaining, sessionContext);
            }
        } else {
            targetOccupant.setAffiliation(newAffiliation);
            
            MucUserItem presenceItem = new MucUserItem(target, null, newAffiliation, Role.None);
            for (Occupant occupant : room.getOccupants()) {
                StanzaBuilder builder = MUCStanzaBuilder.createMessageStanza(room.getJID(), occupant.getJid(), null, null);
                builder.addPreparedElement(presenceItem);

                relayStanza(occupant.getJid(), builder.build(), sessionContext);
            }
        }

        return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .build();
    }

    private Stanza changeRole(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, IqAdminItem item, Room room, Occupant moderator) {
        Occupant target = null;
        if (item.getNick() != null) {
            target = room.findOccupantByNick(item.getNick());
        } else {
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Missing nick for item");
        }
        
        Role newRole = item.getRole();
        // you can not change yourself
        if (moderator.getJid().equals(target.getJid())) {
            return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL, StanzaErrorCondition.CONFLICT);
        }

        // verify change
        if (newRole == Role.None) {
            // a moderator can not kick someone with a higher affiliation
            if (target.getAffiliation().compareTo(moderator.getAffiliation()) < 0) {
                return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        } else if (newRole == Role.Visitor) {
            // moderator, admin and owner can not have their voice revoked
            if (target.getAffiliation() == Affiliation.Admin || target.getAffiliation() == Affiliation.Owner) {
                return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        } else if (newRole == Role.Participant) {
            if (target.getRole() == Role.Moderator) {
                // only admin and owner might revoke moderator
                if (moderator.getAffiliation() != Affiliation.Admin && moderator.getAffiliation() != Affiliation.Owner) {
                    return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                            StanzaErrorCondition.NOT_ALLOWED);
                }
                // admin and owners can not be revoked
                if (target.getAffiliation() == Affiliation.Admin || target.getAffiliation() == Affiliation.Owner) {
                    return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                            StanzaErrorCondition.NOT_ALLOWED);
                }
            }
        } else if (newRole == Role.Moderator) {
            // only admin and owner might grant moderator
            if (moderator.getAffiliation() != Affiliation.Admin && moderator.getAffiliation() != Affiliation.Owner) {
                return MUCHandlerHelper.createErrorReply(getFrom(sessionContext,stanza),stanza, StanzaErrorType.CANCEL,
                        StanzaErrorCondition.NOT_ALLOWED);
            }
        }
        target.setRole(newRole);
        if (newRole == Role.None) {
            // remove user from room
            room.removeOccupant(target.getJid());
        }

        Entity targetInRoom = roomAndNick(room, target);

        Status status = null;
        if (newRole == Role.None) {
            status = new Status(StatusCode.BEEN_KICKED);

            // notify user he got kicked
            Stanza presenceToKicked = MUCStanzaBuilder.createPresenceStanza(null,targetInRoom, target.getJid(),
                    PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, new MucUserItem(
                            Affiliation.None, Role.None),
                    // TODO handle <actor>
                    // TODO handle <reason>
                    status);

            relayStanza(target.getJid(), presenceToKicked, sessionContext);
        }

        PresenceStanzaType availType = (newRole == Role.None) ? PresenceStanzaType.UNAVAILABLE : null;

        // notify remaining users that user got role updated
        MucUserItem presenceItem = new MucUserItem(target.getAffiliation(), newRole);
        for (Occupant occupant : room.getOccupants()) {
            Stanza presenceToRemaining = MUCStanzaBuilder.createPresenceStanza(null,targetInRoom, occupant.getJid(),
                    availType, NamespaceURIs.XEP0045_MUC_USER, presenceItem, status);

            relayStanza(occupant.getJid(), presenceToRemaining, sessionContext);
        }
        return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                .build();
    }

    protected void relayStanza(Entity receiver, Stanza stanza, SessionContext sessionContext) {
        if(!sessionContext.getServerRuntimeContext().relay(stanza)) {
            logger.warn("presence relaying failed");
        }
    }

}
