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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.state.resourcebinding.SessionBindListener;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InMemoryRoomStorageProvider implements RoomStorageProvider, SessionBindListener {

    private Map<RoomKey, Room> rooms = new ConcurrentHashMap<RoomKey, Room>();

    public void initialize() {
        // do nothing
    }

    public Room createRoom(MUCModule module,String nodeName, String name, RoomType... roomTypes) {
        Room room = createInternal(module,nodeName, name, roomTypes);
        rooms.put(room, room);
        return room;
    }
    
    protected Room createInternal(MUCModule module,String nodeName, String name, RoomType... roomTypes) {
        return new Room(module,nodeName, name, roomTypes);
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public Room findRoom(MUCModule module,String roomName) {
        return rooms.get(new RoomKey(module,roomName));
    }

    public boolean roomExists(MUCModule module,String roomName) {
        return rooms.containsKey(new RoomKey(module, roomName));
    }

    public void deleteRoom(Room room) {
        rooms.remove(room);
    }

	@Override
	public void onSessionUnbound(String resource, SessionContext sessionContext) {
		if(rooms.size()>0) {
			Entity fullJid=new EntityImpl(sessionContext.getInitiatingEntity(), resource);
			for(Map.Entry<RoomKey,Room> entry:rooms.entrySet()) {
				Occupant occupant=entry.getValue().findOccupantByJID(fullJid);
				if(occupant!=null) {
					occupant.leaveAsync("unbound");
				}
			}
		}
	}
}
