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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.function.BiFunction;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.CoreError;

/**
 * Errors as summarized in 7.2.17
 * 
 * @author notalexa
 *
 */
public class MUCError extends CoreError {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MUCError(BiFunction<SessionContext,Entity,XMLElement> reason) {
		super(reason);
	}
	
	public static XMLElement jidMalformed(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"modify");
		builder.startInnerElement("jid-malformed",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement notAuthorized(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"auth");
		builder.startInnerElement("not-authorized",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}

	public static XMLElement forbidden(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"auth");
		builder.startInnerElement("forbidden",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement itemNotFound(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"cancel");
		builder.startInnerElement("item-not-found",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement notAllowed(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"cancel");
		builder.startInnerElement("not-allowed",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement notAcceptable(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"cancel");
		builder.startInnerElement("not-acceptable",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement registrationRequired(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"auth");
		builder.startInnerElement("registration-required",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement conflict(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"cancel");
		builder.startInnerElement("conflict",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	public static XMLElement serviceUnavailable(SessionContext sessionContext,Entity room) {
		XMLElementBuilder builder=open(sessionContext,room,"wait");
		builder.startInnerElement("service-unavailable",NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
		return builder.build();
	}
	
	protected static XMLElementBuilder open(SessionContext sessionContext,Entity room,String type) {
		XMLElementBuilder builder=new XMLElementBuilder("error",sessionContext.getSessionNamespaceURI());
		if(room!=null) {
			builder.addAttribute("by",room.toString());
		}
		return builder.addAttribute("type",type);
	}	
}
