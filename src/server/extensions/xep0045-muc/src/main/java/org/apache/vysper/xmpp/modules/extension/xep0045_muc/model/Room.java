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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoDataForm;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.stanza.dataforms.Field.Type;

/**
 * A chat room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Room implements InfoRequestListener, ItemRequestListener {
	private MUCModule module;

    private EnumSet<RoomType> roomTypes;

    private Entity jid;

    private String name;

    private String password;
    
    private boolean rewriteDuplicateNick = true;

    private DiscussionHistory history = new DiscussionHistory();

    private Affiliations affiliations = new Affiliations();

    // keep in a map to allow for quick access
    private Map<Entity, Occupant> occupants = new ConcurrentHashMap<Entity, Occupant>();

    public Room(MUCModule module,Entity jid, String name, RoomType... types) {
        if (jid == null) {
            throw new IllegalArgumentException("JID can not be null");
        } else if (jid.getResource() != null) {
            throw new IllegalArgumentException("JID must be bare");
        }
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Name can not be null or empty");
        }
        this.module=module;
        this.jid = jid;
        this.name = name;

        EnumSet<RoomType> potentialTypes;
        if (types != null && types.length > 0) {
            potentialTypes = EnumSet.copyOf(Arrays.asList(types));

            // make sure the list does not contain antonyms
            RoomType.validateAntonyms(potentialTypes);
        } else {
            potentialTypes = EnumSet.noneOf(RoomType.class);
        }

        // complement with default types
        this.roomTypes = RoomType.complement(potentialTypes);
    }

    public Entity getJID() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public EnumSet<RoomType> getRoomTypes() {
        return roomTypes.clone();
    }

    public boolean isRoomType(RoomType type) {
        return roomTypes.contains(type);
    }

    public boolean rewritesDuplicateNick() {
        return rewriteDuplicateNick;
    }

    public void setRewriteDuplicateNick(boolean rewriteDuplicateNick) {
        this.rewriteDuplicateNick = rewriteDuplicateNick;
    }

    public Occupant addOccupant(Entity occupantJid, String name) {
        Affiliation affiliation = affiliations.getAffiliation(occupantJid);

        // TODO throw a domain specific exception
        if (affiliation == Affiliation.Outcast) {
            throw new RuntimeException("forbidden");
        }

        // default to none
        if (affiliation == null) {
            affiliation = Affiliation.None;
        }
        
        Role role = Role.getRole(affiliation, roomTypes);
        Occupant occupant = new Occupant(occupantJid, name, this, role);
        if (isRoomType(RoomType.MembersOnly) && affiliation == Affiliation.None) {
            // don't add non member to room
            throw new RuntimeException("registration-required");
        } else {
            occupants.put(occupantJid, occupant);
        }
        notifyOccupantAdded(occupant);
        return occupant;
    }

    public Occupant findOccupantByJID(Entity occupantJid) {
        return occupants.get(occupantJid);
    }

    public Occupant findOccupantByNick(String nick) {
        for (Occupant occupant : getOccupants()) {
            if (occupant.getNick().equals(nick))
                return occupant;
        }

        return null;
    }

    public Set<Occupant> getModerators() {
        return getByRole(Role.Moderator);
    }
    
    private Set<Occupant> getByRole(Role role) {
        Set<Occupant> matches = new HashSet<Occupant>();
        for (Occupant occupant : getOccupants()) {
            if (role.equals(occupant.getRole()))
                matches.add(occupant);
        }
        return matches;
    }
    
    public boolean isInRoom(Entity jid) {
        return findOccupantByJID(jid) != null;
    }

    public boolean isInRoom(String nick) {
        return findOccupantByNick(nick) != null;
    }

    public void removeOccupant(Entity occupantJid) {
        Occupant occupant=occupants.remove(occupantJid);
        if(occupant!=null) {
        	notifyOccupantRemoved(occupant);
        }
    }

    public int getOccupantCount() {
        return occupants.size();
    }

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    public Set<Occupant> getOccupants() {
        Set<Occupant> set = new HashSet<Occupant>(occupants.values());
        return Collections.unmodifiableSet(set);
    }

    public List<InfoElement> getInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));

        for (RoomType type : roomTypes) {
            if (type.includeInDisco()) {
                infoElements.add(new Feature(type.getDiscoName()));
            }
        }
        DataForm dataForm=new DataForm();
        dataForm.setType(org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type.result);
        dataForm.addField(new Field(null, Type.HIDDEN, "FORM_TYPE").addValue("http://jabber.org/protocol/muc#roominfo"));
        dataForm.addField(new Field(null, Type.TEXT_SINGLE, "muc#roominfo_description").addValue(name==null?"":name));
        dataForm.addField(new Field(null, Type.TEXT_SINGLE, "muc#roominfo_occupants").addValue(Integer.toString(occupants.size())));
        infoElements.add(new InfoDataForm(dataForm));
        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        // List of users
        List<Item> items = new ArrayList<Item>();

        // TODO is this the right way to determine if the room is private?
        if (isRoomType(RoomType.FullyAnonymous) || isRoomType(RoomType.SemiAnonymous)) {
            // private room, return empty list
        } else {
            for (Occupant occupant : getOccupants()) {
                items.add(new Item(new EntityImpl(getJID(), occupant.getNick())));
            }
        }
        return items;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DiscussionHistory getHistory() {
        return history;
    }

    public Affiliations getAffiliations() {
        return affiliations;
    }
    
    protected void notifyOccupantAdded(Occupant occupant) {	
    }

    protected void notifyOccupantRemoved(Occupant occupant) {
    }
    
    public void leave(Occupant leaving,String reason) {
        for (Occupant occupant : getOccupants()) {
            sendExitRoomPresenceToExisting(leaving, occupant, this, reason, module.getComponentContext());
        }

        if (isRoomType(RoomType.Temporary) && isEmpty()) {
            module.getConference().deleteRoom(getJID());
        }
    }
    
    private void sendExitRoomPresenceToExisting(Occupant exitingOccupant, Occupant existingOccupant, Room room,
            String statusMessage, ServerRuntimeContext serverRuntimeContext) {
        Entity roomAndNewUserNick = new EntityImpl(room.getJID(), exitingOccupant.getNick());

        List<XMLElement> inner = new ArrayList<XMLElement>();
        inner.add(new MucUserItem(null, null, existingOccupant.getAffiliation(), Role.None));

        // is this stanza to be sent to the exiting user himself?
        boolean ownStanza = existingOccupant.getJid().equals(exitingOccupant.getJid());

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

        Stanza presenceToExisting = MUCStanzaBuilder.createPresenceStanza(exitingOccupant,roomAndNewUserNick,
                existingOccupant.getJid(), PresenceStanzaType.UNAVAILABLE, NamespaceURIs.XEP0045_MUC_USER, inner);
        serverRuntimeContext.relay(presenceToExisting);
    }
}
