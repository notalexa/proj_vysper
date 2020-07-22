package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoDataForm;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type;
import org.apache.vysper.xmpp.stanza.dataforms.Field;

public class MUCIqOwnerHandler extends DefaultIQHandler {

	public MUCIqOwnerHandler(Conference conference) {
	}

	@Override
	protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC_OWNER);
	}

	@Override
	protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
		StanzaBuilder reply=StanzaBuilder.createIQStanza(stanza.getTo(),sessionContext.getInitiatingEntity(), IQStanzaType.RESULT, stanza.getID());
		DataForm dataForm=new DataForm();
		dataForm.setType(Type.form);
		dataForm.addField(new Field(null,Field.Type.HIDDEN,"FORM_TYPE","http://jabber.org/protocol/muc#roomconfig"));
		reply.startInnerElement("query",NamespaceURIs.XEP0045_MUC_OWNER);
		new InfoDataForm(dataForm).insertElement(reply);
		reply.endInnerElement();
		Stanza replyStanza=reply.build();
		return replyStanza;
	}

	@Override
	protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
		return StanzaBuilder.createIQStanza(stanza.getTo(),sessionContext.getInitiatingEntity(),IQStanzaType.RESULT, stanza.getID()).build();
	}
}
