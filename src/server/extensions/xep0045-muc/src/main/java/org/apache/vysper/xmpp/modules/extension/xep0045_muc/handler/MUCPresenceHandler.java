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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultPresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCError;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.ServerAdministrationService;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.CoreError;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0045", section = "7.1", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class MUCPresenceHandler extends DefaultPresenceHandler {

    final Logger logger = LoggerFactory.getLogger(MUCPresenceHandler.class);
    private MUCModule module;
    private Conference conference;

    public MUCPresenceHandler(MUCModule module) {
    	this.module=module;
        this.conference = module.getConference();
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        // accept all messages sent to this module
        return true;
    }

    private Stanza error(SessionContext sessionContext,Entity from, Entity to, String id, BiFunction<SessionContext,Entity, XMLElement> inner) {
        StanzaBuilder builder = new StanzaBuilder("presence", sessionContext.getSessionNamespaceURI());
        builder.addAttribute("from", from.getFullQualifiedName());
        builder.addAttribute("to", to.getFullQualifiedName());
        if (id != null) {
            builder.addAttribute("id", id);
        }
        builder.addAttribute("type", "error");
        builder.addPreparedElement(new X());
        builder.addPreparedElement(inner.apply(sessionContext,from.getBareJID()));
		return builder.build();
	}

	@Override
    protected Stanza executePresenceLogic(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        // TODO handle null
        Entity roomAndNick = stanza.getTo();

        Entity occupantJid = sessionContext.getFrom(stanza);//stanza.getFrom();

        Entity roomJid = roomAndNick.getBareJID();
        String nick = roomAndNick.getResource();

        // user did not send nick name
        if (nick == null) {
        	return error(sessionContext,roomJid,occupantJid,stanza.getID(),MUCError::jidMalformed);
        }

        String type = stanza.getType();
        if (type == null) {
            return available(sessionContext,stanza, roomJid, occupantJid, nick, serverRuntimeContext);
        } else if (type.equals("unavailable")) {
            return unavailable(sessionContext,stanza, roomJid, occupantJid, nick, serverRuntimeContext);
        } else {
            throw new RuntimeException("Presence type not handled by MUC module: " + type);
        }

    }

    private String getInnerElementText(XMLElement element, String childName) {
        try {
            XMLElement childElm = element.getSingleInnerElementsNamed(childName);
            if (childElm != null && childElm.getInnerText() != null) {
                return childElm.getInnerText().getText();
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            return null;
        }
    }

    protected void sendJoinSequence(SessionContext sessionContext,Stanza stanza,Occupant occupant) {
    	Room room=occupant.getRoom();
        for (Occupant existingOccupant : room.getOccupants()) {
            sendExistingOccupantToNewOccupant(occupant, existingOccupant, room, room.getModule().getComponentContext());
        }

        // relay presence of the newly added occupant to all existing occupants
        for (Occupant existingOccupant : room.getOccupants()) {
            sendNewOccupantPresenceToExisting(occupant, existingOccupant, room, room.getModule().getComponentContext(), false,false);
        }
        // send discussion history to user
        boolean includeJid = room.isRoomType(RoomType.NonAnonymous);
        List<Stanza> history = room.getHistory().createStanzas(occupant, includeJid, History.fromStanza(stanza));
        relayStanzas(occupant.getJid(), history, room.getModule().getComponentContext());
        
        Stanza subject=getSubjectStanza(sessionContext,occupant);
        if(subject!=null) {
        	relayStanza(occupant.getJid(), subject, room.getModule().getComponentContext());
        }
    }
    
    private Stanza available(SessionContext sessionContext,PresenceStanza stanza, Entity roomJid, Entity newOccupantJid, String nick,
            ServerRuntimeContext serverRuntimeContext) {

        boolean newRoom = false;
        // TODO what to use for the room name?
        Room room = conference.findRoom(roomJid.getNode());
        if(room == null) {
        	// NonAnonymous is for Jitsi. Need to handle this in the owner handler
            room = conference.createRoom(roomJid.getNode(), roomJid.getNode(),RoomType.NonAnonymous);
            newRoom = true;
        }
        X x = X.fromStanza(stanza);
        if (room.isInRoom(newOccupantJid)) {
            Occupant occupant = room.findOccupantByJID(newOccupantJid);
        	if(x!=null&&x.isEmpty()) {
                logger.debug("{} has requested to rejoin in room {}", newOccupantJid, roomJid);
                sendJoinSequence(sessionContext, stanza, occupant);
//                // relay presence of all existing room occupants to the now joined occupant
//                for (Occupant existingOccupant : room.getOccupants()) {
//                    sendExistingOccupantToNewOccupant(occupant, existingOccupant, room, serverRuntimeContext);
//                }
//
//                // relay presence of the newly added occupant to all existing occupants
//                for (Occupant existingOccupant : room.getOccupants()) {
//                    sendNewOccupantPresenceToExisting(occupant, existingOccupant, room, serverRuntimeContext, false,false);
//                }
//                // send discussion history to user
//                boolean includeJid = room.isRoomType(RoomType.NonAnonymous);
//                List<Stanza> history = room.getHistory().createStanzas(occupant, includeJid, History.fromStanza(stanza));
//                relayStanzas(newOccupantJid, history, serverRuntimeContext);
//                
//                Stanza subject=getSubjectStanza(sessionContext,room);
//                if(subject!=null) {
//                	relayStanza(newOccupantJid, stanza, serverRuntimeContext);
//                }
        	} else {
	            // user is already in room, change nick
	            logger.debug("{} has requested to change nick in room {}", newOccupantJid, roomJid);
	            // occupant is already in room
	            occupant.updatePrivate(stanza);
	            if (nick.equals(occupant.getNick())) {
	                // nick unchanged, change show and status
	                for (Occupant receiver : room.getOccupants()) {
	                    sendChangeShowStatus(occupant, receiver, room, getInnerElementText(stanza, "show"),
	                            getInnerElementText(stanza, "status"), serverRuntimeContext);
	                }
	            } else {
	                if (room.isInRoom(nick)) {
	                    // user with this nick is already in room
	                	return error(sessionContext,roomJid,newOccupantJid,stanza.getID(),MUCError::conflict);
	                }
	
	                String oldNick = occupant.getNick();
	                // update the nick
	                occupant.setNick(nick);
	
	                // send out unavailable presences to all existing occupants
	                for (Occupant receiver : room.getOccupants()) {
	                    sendChangeNickUnavailable(occupant, oldNick, receiver, room, serverRuntimeContext);
	                }
	
	                // send out available presences to all existing occupants
	                for (Occupant receiver : room.getOccupants()) {
	                    sendChangeNickAvailable(occupant, receiver, room, serverRuntimeContext);
	                }
	
	            }
        	}
        } else {
            logger.debug("{} has requested to enter room {}", newOccupantJid, roomJid);
            if(x==null) {
            	// 7.2.18
            	List<XMLElement> inner=new ArrayList<XMLElement>();
            	inner.add(new MucUserItem(Affiliation.None,Role.None));
            	inner.add(new Status(StatusCode.OWN_PRESENCE));
            	inner.add(new Status(StatusCode.BEEN_KICKED));
            	inner.add(new Status(StatusCode.REMOVED_BY_ERROR));
            	return MUCStanzaBuilder.createPresenceStanza(null,newOccupantJid,
    	                sessionContext.getInitiatingEntity(), PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);
            }

            boolean nickConflict = room.isInRoom(nick);
            if (nickConflict) {
            	// 7.2.8
            	return error(sessionContext,roomJid,newOccupantJid,stanza.getID(),MUCError::conflict);
            }

            // check password if password protected
            if (room.isRoomType(RoomType.PasswordProtected)) {
                String password = null;
                if (x != null) {
                    password = x.getPasswordValue();
                }

                if (password == null || !password.equals(room.getPassword())) {
                    // 7.2.5 password missing or not matching
                    return error(sessionContext,roomJid,newOccupantJid,stanza.getID(),MUCError::notAuthorized);
                }
            }

            Occupant newOccupant;
            try {
                newOccupant = room.addOccupant(newOccupantJid, nick);
            } catch(CoreError e) {
            	return error(sessionContext,roomJid,newOccupantJid,stanza.getID(),e.getReason());
            } catch(RuntimeException e) {
                return error(sessionContext,roomJid,newOccupantJid,stanza.getID(),MUCError::notAuthorized);
            }
            boolean nickRewritten=!nick.equals(newOccupant.getNick());
            newOccupant.updatePrivate(stanza);
            if(newRoom) {
                newOccupant.setAffiliation(Affiliation.Owner);
                newOccupant.setRole(Role.Moderator);
            }

            // if the new occupant is a server admin, he will be for the room, too
            final ServerAdministrationService adhocCommandsService = (ServerAdministrationService)serverRuntimeContext.getServerRuntimeContextService(ServerAdministrationService.SERVICE_ID);
            if (adhocCommandsService != null && adhocCommandsService.isAdmin(newOccupantJid.getBareJID())) {
                final Affiliation roomAffiliation = room.getAffiliation(newOccupantJid);
                // make new occupant an Admin, but do not downgrade from Owner
                // Admin affilitation implies Moderator role (see XEP-0045 5.1.2)
                if (room.getAffiliation(newOccupantJid) != Affiliation.Owner) {
                    newOccupant.setAffiliation(Affiliation.Admin);
                    newOccupant.setRole(Role.Moderator);
                }
            }
            sendJoinSequence(sessionContext, stanza, newOccupant);
//            
//            // relay presence of all existing room occupants to the now joined occupant
//            for (Occupant occupant : room.getOccupants()) {
//                sendExistingOccupantToNewOccupant(newOccupant, occupant, room, serverRuntimeContext);
//            }
//
//            // relay presence of the newly added occupant to all existing occupants
//            for (Occupant occupant : room.getOccupants()) {
//                sendNewOccupantPresenceToExisting(newOccupant, occupant, room, serverRuntimeContext, nickRewritten,newRoom);
//            }
//
//            // send discussion history to user
//            boolean includeJid = room.isRoomType(RoomType.NonAnonymous);
//            List<Stanza> history = room.getHistory().createStanzas(newOccupant, includeJid, History.fromStanza(stanza));
//            relayStanzas(newOccupantJid, history, serverRuntimeContext);
//            
//            Stanza subject=getSubjectStanza(sessionContext,room);
//            if(subject!=null) {
//            	relayStanza(newOccupantJid, stanza, serverRuntimeContext);
//            }
            logger.debug("{} successfully entered room {}", newOccupantJid, roomJid);
        }
        return null;
    }

    /**
     * Specifiec in 7.2.15 (Room Subject);
     * @param context
     * @param room
     * @return
     */
    protected Stanza getSubjectStanza(SessionContext context,Occupant occupant) {
    	String subject=occupant.getRoom().getSubject();
    	if(subject==null) {
    		subject="";
    	}
    	return new StanzaBuilder("message", context.getSessionNamespaceURI())
        	.addAttribute("from", occupant.getRoom().getJID().getFullQualifiedName())
            .addAttribute("to", occupant.getJid().getFullQualifiedName())
            .addAttribute("type","groupchat")
            .startInnerElement("subject", context.getSessionNamespaceURI()).addText(subject).endInnerElement().build();
    }

    private Stanza unavailable(SessionContext sessionContext,PresenceStanza stanza, Entity roomJid, Entity occupantJid, String nick,
            ServerRuntimeContext serverRuntimeContext) {
        Room room = conference.findRoom(roomJid.getNode());

        // room must exist, or we do nothing
        if (room != null) {
            Occupant exitingOccupant = room.findOccupantByJID(occupantJid);

            // user must by in room, or we do nothing
            if (exitingOccupant != null) {
                String statusMessage = null;
                try {
                    XMLElement statusElement = stanza.getSingleInnerElementsNamed("status");
                    if (statusElement != null && statusElement.getInnerText() != null) {
                        statusMessage = statusElement.getInnerText().getText();
                    }
                } catch (XMLSemanticError e) {
                    // ignore, status element did not exist
                }
            	exitingOccupant.leave(statusMessage);
            }
        }

        return null;
    }

    private void sendExistingOccupantToNewOccupant(Occupant newOccupant, Occupant existingOccupant, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        //            <presence
        //            from='darkcave@chat.shakespeare.lit/firstwitch'
        //            to='hag66@shakespeare.lit/pda'>
        //          <x xmlns='http://jabber.org/protocol/muc#user'>
        //            <item affiliation='owner' role='moderator'/>
        //          </x>
        //        </presence>

        // do not send own presence
        if (existingOccupant.getJid().equals(newOccupant.getJid())) {
            return;
        }

        Entity roomAndOccupantNick = new EntityImpl(room.getJID(), existingOccupant.getNick());
        Stanza presenceToNewOccupant = MUCStanzaBuilder.createPresenceStanza(existingOccupant,roomAndOccupantNick, newOccupant.getJid(),
                null, NamespaceURIs.XEP0045_MUC_USER, new MucUserItem(existingOccupant,true,false));
        relayStanza(newOccupant.getJid(), presenceToNewOccupant, serverRuntimeContext);
    }

    private void sendNewOccupantPresenceToExisting(Occupant newOccupant, Occupant existingOccupant, Room room,
                                                   ServerRuntimeContext serverRuntimeContext, boolean nickRewritten,boolean newRoom) {
    	MucUserItem item=newOccupant.getMucUserItem(existingOccupant);
    	if(item!=null) {
	        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), newOccupant.getNick());
	
	        List<XMLElement> inner = new ArrayList<XMLElement>();
	        inner.add(item);
	
	        if (existingOccupant.getJid().equals(newOccupant.getJid())) {
	
	            if (room.isRoomType(RoomType.NonAnonymous)) {
	                // notify thnee user that this is a non-anonymous room
	                inner.add(new Status(StatusCode.ROOM_NON_ANONYMOUS));
	            }
	
	            // send status to indicate that this is the users own presence
	            if (nickRewritten) inner.add(new Status(StatusCode.NICK_MODIFIED));
	            if(newRoom) {
	            	inner.add(new Status(StatusCode.ROOM_CREATED));
	            	if(room.hasPublicArchive()) {
	            		inner.add(new Status(StatusCode.ROOM_LOGGING_ENABLED));
	            	}
	            }
	            inner.add(new Status(StatusCode.OWN_PRESENCE));
	        }
	
	        Stanza presenceToExisting = MUCStanzaBuilder.createPresenceStanza(newOccupant,roomAndNewUserNick,
	                existingOccupant.getJid(), null, NamespaceURIs.XEP0045_MUC_USER, inner);
	
	        logger.debug("Room presence from {} sent to {}", roomAndNewUserNick, existingOccupant);
	        relayStanza(existingOccupant.getJid(), presenceToExisting, serverRuntimeContext);
    	}
    }

    private void sendChangeNickUnavailable(Occupant changer, String oldNick, Occupant receiver, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndOldNick = new EntityImpl(room.getJID(), oldNick);

        List<XMLElement> inner = new ArrayList<XMLElement>();

        boolean includeJid = includeJidInItem(room, receiver);
        inner.add(new MucUserItem(changer, includeJid, true));
        inner.add(new Status(StatusCode.NEW_NICK));

//        if (receiver.getJid().equals(changer.getJid())) {
//            // send status to indicate that this is the users own presence
//            inner.add(new Status(StatusCode.OWN_PRESENCE));
//        }
        Stanza presenceToReceiver = MUCStanzaBuilder.createPresenceStanza(changer,roomAndOldNick, receiver.getJid(),
                PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);
        logger.debug("Room presence from {} sent to {}", roomAndOldNick, receiver);
        relayStanza(receiver.getJid(), presenceToReceiver, serverRuntimeContext);
    }

    private void sendChangeShowStatus(Occupant changer, Occupant receiver, Room room, String show, String status,
            ServerRuntimeContext serverRuntimeContext) {
    	MucUserItem item=changer.getMucUserItem(receiver);
    	if(item!=null) {
	        Entity roomAndNick = new EntityImpl(room.getJID(), changer.getNick());
	
	        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(changer.getJidInRoom(), receiver.getJid(), null, null, show, status);
	        changer.buildStanza(builder);
	        List<XMLElement> inner;
	        if(changer==receiver) {
	        	inner=new ArrayList<XMLElement>();
	        	inner.add(item);
	        	inner.add(new Status(StatusCode.OWN_PRESENCE));
	        } else {
	        	inner=Collections.singletonList(item);
	        }
	        builder.addPreparedElement(new X(NamespaceURIs.XEP0045_MUC_USER,inner));
	
	        logger.debug("Room presence from {} sent to {}", roomAndNick, receiver);
	        relayStanza(receiver.getJid(), builder.build(), serverRuntimeContext);
    	}
    }

    private boolean includeJidInItem(Room room, Occupant receiver) {
        // room is non-anonymous or semi-anonymous and the occupant a moderator, send full user JID
        return room.isRoomType(RoomType.NonAnonymous)
                || (room.isRoomType(RoomType.SemiAnonymous) && receiver.getRole() == Role.Moderator);
    }

    private void sendChangeNickAvailable(Occupant changer, Occupant receiver, Room room,
            ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndOldNick = new EntityImpl(room.getJID(), changer.getNick());

        List<XMLElement> inner = new ArrayList<XMLElement>();
        boolean includeJid = includeJidInItem(room, receiver);
        inner.add(new MucUserItem(changer, includeJid, false));

        if (receiver.getJid().equals(changer.getJid())) {
            // send status to indicate that this is the users own presence
            inner.add(new Status(StatusCode.OWN_PRESENCE));
        }
        Stanza presenceToReceiver = MUCStanzaBuilder.createPresenceStanza(changer,roomAndOldNick, receiver.getJid(), null,
                NamespaceURIs.XEP0045_MUC_USER, inner);

        relayStanza(receiver.getJid(), presenceToReceiver, serverRuntimeContext);
    }

    protected void relayStanzas(Entity receiver, List<Stanza> stanzas, ServerRuntimeContext serverRuntimeContext) {
        for (Stanza stanza : stanzas) {
            relayStanza(receiver, stanza, serverRuntimeContext);
        }
    }

    protected void relayStanza(Entity receiver, Stanza stanza, ServerRuntimeContext serverRuntimeContext) {
        if(!serverRuntimeContext.relay(stanza)) {
            logger.warn("presence relaying failed");
        }
    }
}
