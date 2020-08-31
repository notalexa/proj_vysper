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
package org.apache.vysper.xmpp.server.response;

import java.util.function.BiFunction;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.SessionContext;

public class CoreError extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected BiFunction<SessionContext,Entity,XMLElement> reason;
	
	public CoreError(BiFunction<SessionContext,Entity,XMLElement> reason) {
		this.reason=reason;
	}
	
	public BiFunction<SessionContext,Entity,XMLElement> getReason() {
		return reason;
	}

}
