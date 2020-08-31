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

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.state.resourcebinding.SessionBindListener;

public interface RoomStorageProvider extends StorageProvider, SessionBindListener {

    void initialize();

    Room createRoom(MUCModule module,String nodeName, String name, RoomType... roomTypes);

    Collection<Room> getAllRooms();

    boolean roomExists(MUCModule module,String nodeName);

    Room findRoom(MUCModule module,String nodeName);

    void deleteRoom(Room room);
    
    public class RoomKey {
    	protected final MUCModule module;
    	protected final String nodeName;
    	public RoomKey(MUCModule module,String nodeName) {
    		this.module=module;
    		this.nodeName=nodeName;
    	}
    	
    	public int hashCode() {
    		return module.hashCode()^nodeName.hashCode();
    	}
    	
    	public boolean equals(Object o) {
    		if(o instanceof RoomKey) {
    			RoomKey other=(RoomKey)o;
    			return other.module.equals(module)&&other.nodeName.equals(nodeName);
    		}
    		return false;
    	}
    	
    	public MUCModule getModule() {
    		return module;
    	}
    	
    	public String getNodeName() {
    		return nodeName;
    	}
    }
}
