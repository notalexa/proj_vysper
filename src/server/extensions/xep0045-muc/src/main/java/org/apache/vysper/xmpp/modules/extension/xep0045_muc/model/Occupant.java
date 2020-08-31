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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * An occupant (user) in a room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Occupant {

    private Room room;
    
    private Affiliation affiliation;
    
    private Role role;

    private Entity jid;

    private String nick;
    
    private Map<String,XMLElement> privateData=new HashMap<>();

    public Occupant(Entity jid, String nick, Room room, Affiliation affiliation, Role role) {
        if (jid == null)
            throw new IllegalArgumentException("JID can not be null");
        if (nick == null)
            throw new IllegalArgumentException("Name can not be null");
        if (room == null)
            throw new IllegalArgumentException("Room can not be null");
        if (role == null)
            throw new IllegalArgumentException("Role can not be null");

        this.jid = jid;
        this.affiliation=affiliation;
        this.nick = nick;
        this.room = room;
        this.role = role;
    }

    public Affiliation getAffiliation() {
        return affiliation==null?Affiliation.None:affiliation;
    }
    
    public Room getRoom() {
    	return room;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Entity getJid() {
        return jid;
    }

    public boolean hasVoice() {
        return role == Role.Moderator || role == Role.Participant;
    }

    @Override
    public String toString() {
        return jid.getFullQualifiedName();
    }

    public boolean isModerator() {
        return role == Role.Moderator;
    }
    
    public Entity getJidInRoom() {
        return new EntityImpl(room.getJID(), nick);
    }
    
    public void updatePrivate(PresenceStanza stanza) {
    	for(XMLElement inner:stanza.getInnerElements()) {
    		if(!inner.getNamespaceURI().equals(stanza.getNamespaceURI())&&!"x".equals(inner.getName())) {
    			privateData.put(inner.getName(),inner);
    		}
    	}
    }
    
    public void buildStanza(StanzaBuilder builder) {
    	for(XMLElement inner:privateData.values()) {
    		builder.addPreparedElement(inner);
    	}
    }
    
    public XMLElement getData(String tag) {
    	return privateData.get(tag);
    }
    
    public Collection<XMLElement> getData() {
    	return privateData.values();
    }

	public void setAffiliation(Affiliation affiliation) {
		this.affiliation=affiliation;		
	}
	
	/**
	 * Spec Chapter 7.14.
	 * 
	 * @param module the module the room belongs
	 * @param statusMessage
	 */
	public void leave(String statusMessage) {
        role=Role.None;
        Set<Occupant> occupants=room.getOccupants();
        room.removeOccupant(jid);
        for (Occupant occupant : occupants) {
            sendExitRoomPresenceToExisting(occupant, statusMessage, room.getModule().getComponentContext());
        }
        if (room.isRoomType(RoomType.Temporary) && room.isEmpty()) {
            room.getModule().getConference().deleteRoom(room);
        }
	}
	
	public void leaveAsync(String statusMessage) {
		new Thread() {
			public void run() {
				leave(statusMessage);
			}
		}.start();
	}
	
	/**
	 * Should we broadcast presence to the target. This is configured via <code>muc#roomconfig_presencebroadcast</code> (and stored in {@link #presenceBroadcastSet}).
	 * The method also returns <code>true</code> if the target is this occupant (sending presence to himself in any room).
	 * 
	 * @param target the occupant the presence stanza should be send
	 * @return <code>true</code> if a presence stanza should be send
	 */
	protected boolean doBroadcastPresenceTo(Occupant target) {
		return target==this||room.doBroadcastPresenceTo(target);
	}
	
	/**
	 * Method handles non anonymous (XEP 0045 7.2.3) and semi anonymous (XEP 0045 7.2.4) rooms. The method also reflects the configuration
	 * of <code>muc#roomconfig_presencebroadcast</code> reflected in {@link #doBroadcastPresenceTo(Occupant)} (and {@link #presenceBroadcastSet}.
	 * 
	 * @param target the target of the stanza
	 * @return the muc user item for the given target or <code>null</code> if no presence item should be send
	 */
	public MucUserItem getMucUserItem(Occupant target) {
		if(doBroadcastPresenceTo(target)) {
			return new MucUserItem(room.isRoomType(RoomType.NonAnonymous)||(target.getRole()==Role.Moderator&&room.isRoomType(RoomType.SemiAnonymous))?getJid():null,null, getAffiliation(), role);
		} else {
			return null;
		}
	}
	
    private void sendExitRoomPresenceToExisting(Occupant target, String statusMessage, ServerRuntimeContext serverRuntimeContext) {
    	MucUserItem item=getMucUserItem(target);
    	if(item!=null) {
	        List<XMLElement> inner = new ArrayList<XMLElement>();
	        inner.add(item);
	
	        // is this stanza to be sent to the exiting user himself?
	        boolean ownStanza = target.getJid().equals(getJid());
	
	        if (ownStanza || statusMessage != null) {
	
	            Status status;
	            if (ownStanza) {
	                // send status to indicate that this is the users own presence
	                status = new Status(StatusCode.OWN_PRESENCE, statusMessage);
	            } else {
	                status = new Status(statusMessage);
	            }
	            inner.add(status);
	        }
	
	        Stanza presenceToExisting = MUCStanzaBuilder.createPresenceStanza(null,this.getJidInRoom(),
	                target.getJid(), PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);
	        serverRuntimeContext.relay(presenceToExisting);
    	}
    }
}
