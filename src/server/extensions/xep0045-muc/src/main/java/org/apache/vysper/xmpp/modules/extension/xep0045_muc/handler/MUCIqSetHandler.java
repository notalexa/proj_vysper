package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainerImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

public class MUCIqSetHandler implements StanzaHandler {
	protected Conference conference;
	public MUCIqSetHandler(Conference conference) {
		this.conference=conference;
	}

	@Override
	public String getName() {
		return "muc.iq set";
	}

	@Override
	public boolean verify(Stanza stanza) {
		return true;
	}

	@Override
	public boolean isSessionRequired() {
		return true;
	}

	@Override
	public ResponseStanzaContainer execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
			boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder)
			throws ProtocolException {
		if(isOutboundStanza) {
			Entity to=stanza.getTo();
			if(to.isNodeSet()&&to.isResourceSet()) {
				Room room=conference.findRoom(to.getBareJID());
				if(room!=null) {
					Occupant occupant=room.findOccupantByNick(to.getResource());
					if(occupant!=null) try {
						Entity from=stanza.getFrom();
						if(from==null) {
							Occupant fromOccupant=room.findOccupantByJID(sessionContext.getInitiatingEntity());
							if(fromOccupant==null) {
								for(Occupant o:room.getOccupants()) {
									if(o.getJid().getBareJID().equals(sessionContext.getInitiatingEntity())) {
										fromOccupant=o;
										break;
									}
								}
							}
							if(fromOccupant!=null) {
								stanza=StanzaBuilder.createForwardStanza(stanza, fromOccupant.getJidInRoom(), null);
							} else {
								return null;
							}
						}
			            serverRuntimeContext.relay(stanza);
			            return null;
					} catch(Throwable t) {
						t.printStackTrace();
					}
				}
				
			}
		}
        return new ResponseStanzaContainerImpl(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, IQStanza.getWrapper(stanza),
                StanzaErrorType.CANCEL, "iq stanza of type 'set' is not handled for this namespace",
                serverRuntimeContext.getDefaultXMLLang(), null));
	}

}
