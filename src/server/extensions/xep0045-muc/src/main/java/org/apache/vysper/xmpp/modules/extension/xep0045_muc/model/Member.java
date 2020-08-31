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

import org.apache.vysper.xmpp.addressing.Entity;

public class Member {
	protected Entity jid;
	protected Affiliation affiliation;
	protected String nick;
	protected boolean persistent;
	
	public Member(Entity jid,Affiliation affiliation) {
		this(jid,affiliation,null);
	}
	
	public Member(Entity jid,Affiliation affiliation,String nick) {
		this(jid,affiliation,nick,true);
	}
	public Member(Entity jid,Affiliation affiliation,String nick,boolean persistent) {
		this.jid=jid;
		this.affiliation=affiliation;
		this.nick=nick;
		this.persistent=persistent;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	
	public Affiliation getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(Affiliation affiliation) {
		this.affiliation = affiliation;
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
}
