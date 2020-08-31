/*
 * Copyright (C) 2020 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.Arrays;

public class RoomSettings {
	private static final int TYPES=ConfigurationType.values().length;
	private static final RoomType[] DEFAULTS=new RoomType[] {
		RoomType.FullyAnonymous,
		RoomType.Unsecured,
		RoomType.Temporary,
		RoomType.Open,
		RoomType.Unmoderated,
		RoomType.Public,
		RoomType.ModeratedSubject
	};
	
	private static RoomType[] set(RoomType[] settings,RoomType...types) {
		if(settings==null) {
			settings=set(new RoomType[TYPES],DEFAULTS);
		}
		for(RoomType type:types) {
			settings[type.getTypeCategory().ordinal()]=type;
		}
		return settings;
	}
	
	private RoomType[] settings=new RoomType[TYPES];
	
	public RoomSettings(RoomType...types) {
		settings=set(set(null),types);
	}
	
	public RoomSettings modify(RoomType...types) {
		set(settings,types);
		return this;
	}
	
	public RoomType get(ConfigurationType type) {
		return settings[type.ordinal()];
	}
	
	public boolean contains(RoomType type) {
		return settings[type.getTypeCategory().ordinal()]==type;
	}
	
	public RoomType[] getTypes() {
		return settings.clone();
	}
	
	public static void main(String[] args) {
		System.out.println(Arrays.toString(set(null)));
	}
}
