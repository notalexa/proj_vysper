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

package org.apache.vysper.xmpp.extension.xep0114.handler;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StreamStartHandler implements StanzaHandler {
    public String getName() {
        return "stream";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        String namespaceURI = stanza.getNamespaceURI();
        if (namespaceURI == null)
            return false;
        return namespaceURI.equals(NamespaceURIs.JABBER_CLIENT) || namespaceURI.equals(NamespaceURIs.JABBER_SERVER);
    }

    public boolean isSessionRequired() {
        return true;
    }

    public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        boolean jabberNamespace = NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS.equals(stanza.getNamespaceURI());

        boolean clientCall = xmlElementVerifier.namespacePresent(NamespaceURIs.JABBER_CLIENT);
        boolean serverCall = xmlElementVerifier.namespacePresent(NamespaceURIs.JABBER_SERVER);

        // TODO is it better to derive c2s or s2s from the type of endpoint and verify the namespace here?
        if (clientCall && serverCall)
            serverCall = false; // silently ignore ambiguous attributes
        if (serverCall)
            sessionContext.setServerToServer();
        else
            sessionContext.setClientToServer();

        if (sessionStateHolder.getState() != SessionState.INITIATED) {
            return respondUnsupportedStanzaType("unexpected stream start");
        }

        // http://etherx.jabber.org/streams cannot be omitted
        if (!jabberNamespace) {
            return respondIllegalNamespaceError("namespace is mandatory: "
                    + NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS);
        }
        
        Stanza responseStanza=getStreamOpener(stanza.getTo(), sessionContext.getSessionId(), null).build();

        if (responseStanza != null)
            return new ResponseStanzaContainerImpl(responseStanza);

        return null;
    }

    private ResponseStanzaContainer respondIllegalNamespaceError(String descriptiveText) {
        return new ResponseStanzaContainerImpl(ServerErrorResponses.getStreamError(
                StreamErrorCondition.INVALID_NAMESPACE, null, descriptiveText, null));
    }

    private ResponseStanzaContainer respondUnsupportedStanzaType(String descriptiveText) {
        return new ResponseStanzaContainerImpl(ServerErrorResponses.getStreamError(
                StreamErrorCondition.UNSUPPORTED_STANZA_TYPE, null, descriptiveText, null));
    }
    
    public StanzaBuilder getStreamOpener(Entity from,
            String sessionId, XMLElement innerStanza) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS,
                "stream")
                .addAttribute("from", from.getFullQualifiedName());
        if (sessionId != null)
            stanzaBuilder.addAttribute("id", sessionId);
        if (innerStanza != null)
            stanzaBuilder.addPreparedElement(innerStanza);
        return stanzaBuilder;
    }


}
