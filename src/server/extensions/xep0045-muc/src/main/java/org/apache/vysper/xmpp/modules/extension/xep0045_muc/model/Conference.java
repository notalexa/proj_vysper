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
import java.util.List;

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryOccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryRoomStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * Represents the root of a conference, containing rooms
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Conference implements ServerInfoRequestListener, ItemRequestListener {
	public static final String DEFAULT_NAME="Conference";
	private MUCModule module;
    private String name;
    private String conferenceServer;

    private RoomStorageProvider roomStorageProvider = new InMemoryRoomStorageProvider();

    private OccupantStorageProvider occupantStorageProvider = new InMemoryOccupantStorageProvider();

    public Conference(String name) {
    	this(name,null);
    }
    public Conference(String name,String conferenceServer) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Name must not be null or empty");
        }
        this.conferenceServer=conferenceServer;
        this.name = name;
    }

    public void initialize(MUCModule module,StorageProviderRegistry registry) {
    	this.module=module;
    	this.roomStorageProvider=(RoomStorageProvider)registry.retrieve(RoomStorageProvider.class);
    	if(roomStorageProvider==null) {
    		roomStorageProvider=new InMemoryRoomStorageProvider();
    	}
    	occupantStorageProvider=(OccupantStorageProvider)registry.retrieve(OccupantStorageProvider.class);
    	if(occupantStorageProvider==null) {
    		occupantStorageProvider=new InMemoryOccupantStorageProvider();
    	}
    }
    
    

    public Collection<Room> getAllRooms() {
    	List<Room> rooms=new ArrayList<Room>();
    	for(Room room:roomStorageProvider.getAllRooms()) {
    		if(room.getModule()==module) {
    			rooms.add(room);
    		}
    	}
        return rooms;
    }

    public Room createRoom(String nodeName, String name, RoomType... types) {
        if (roomStorageProvider.roomExists(module,nodeName)) {
            throw new IllegalArgumentException("Room already exists with name: " + nodeName);
        }

        return roomStorageProvider.createRoom(module,nodeName, name, types);
    }

    public void deleteRoom(Room room) {
    	roomStorageProvider.deleteRoom(room);
    }

    public Room findRoom(String nodeName) {
        return roomStorageProvider.findRoom(module,nodeName);
    }

    public Room findOrCreateRoom(String nodeName, String name, RoomType... types) {
        Room room = findRoom(nodeName);
        if (room == null) {
            room = createRoom(nodeName, name, types);
        }
        return room;
    }

    public OccupantStorageProvider getOccupantStorageProvider() {
        return occupantStorageProvider;
    }

    public void setOccupantStorageProvider(OccupantStorageProvider occupantStorageProvider) {
        this.occupantStorageProvider = occupantStorageProvider;
    }

    public RoomStorageProvider getRoomStorageProvider() {
        return roomStorageProvider;
    }

    public void setRoomStorageProvider(RoomStorageProvider roomStorageProvider) {
        this.roomStorageProvider = roomStorageProvider;
    }

    public String getName() {
        return name;
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        if (request.getNode() != null && request.getNode().length() > 0) return null;
        
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("conference", "text", getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0045_MUC));
        return infoElements;
    }

    public List<Item> getItemsFor(InfoRequest request) {
        List<Item> items = new ArrayList<Item>();
        Collection<Room> rooms = getAllRooms();

        for (Room room : rooms) {
            items.add(new Item(module.getRoomJid(room), room.getName()));
        }

        return items;
    }

	public String resolveConferenceURL(String nodeName) {
		return conferenceServer==null?null:(conferenceServer+"/"+nodeName);
	}

}
