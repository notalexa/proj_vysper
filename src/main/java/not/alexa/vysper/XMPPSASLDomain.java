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
package not.alexa.vysper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.cryptography.BogusTrustManagerFactory;
import org.apache.vysper.xmpp.cryptography.InputStreamBasedTLSContextFactory;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringInternalInboundStanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.OfflineStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.modules.roster.RosterModule;
import org.apache.vysper.xmpp.modules.servicediscovery.ServiceDiscoveryModule;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContextProvider;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

public class XMPPSASLDomain {
    private final List<SASLMechanism> saslMechanisms = new ArrayList<SASLMechanism>();
    private String serverDomain;
    private DefaultServerRuntimeContext serverRuntimeContext;
    private InputStream tlsCertificate;
    private String tlsCertificatePassword;
    private List<Module> modules=new ArrayList<Module>();
    
    public XMPPSASLDomain(String serverDomain) {
    	this.serverDomain=serverDomain;
        modules.add(new ServiceDiscoveryModule());
        modules.add(new RosterModule());
    }

    public ServerRuntimeContext start(ServerRuntimeContextProvider contextProvider,StorageProviderRegistry storageProviderRegistry,ResourceRegistry resourceRegistry) throws IOException {
        BogusTrustManagerFactory bogusTrustManagerFactory = new BogusTrustManagerFactory();
        InputStreamBasedTLSContextFactory tlsContextFactory = new InputStreamBasedTLSContextFactory(tlsCertificate);
        tlsContextFactory.setPassword(tlsCertificatePassword);
        tlsContextFactory.setTrustManagerFactory(bogusTrustManagerFactory);

        EntityImpl serverEntity = new EntityImpl(null, serverDomain, null);
        AccountManagement accountManagement = (AccountManagement) storageProviderRegistry
                .retrieve(AccountManagement.class);
        OfflineStanzaReceiver offlineReceiver = (OfflineStanzaReceiver) storageProviderRegistry.retrieve(OfflineStorageProvider.class);
        DeliveringInternalInboundStanzaRelay stanzaRelay = new DeliveringInternalInboundStanzaRelay(serverEntity,
                resourceRegistry, accountManagement,offlineReceiver);
        List<HandlerDictionary> dictionaries = new ArrayList<HandlerDictionary>();
        addCoreDictionaries(dictionaries);

        ServerFeatures serverFeatures = new ServerFeatures();
        serverFeatures.setAuthenticationMethods(saslMechanisms);

        serverRuntimeContext = new DefaultServerRuntimeContext(serverEntity, contextProvider, stanzaRelay, serverFeatures,
                dictionaries, resourceRegistry);
        serverRuntimeContext.setStorageProviderRegistry(storageProviderRegistry);
        serverRuntimeContext.setTlsContextFactory(tlsContextFactory);
        
        for(Module module:modules) {
        	serverRuntimeContext.addModule(module);
        }

        stanzaRelay.setServerRuntimeContext(serverRuntimeContext);
        return serverRuntimeContext;
    }
    
    private void addCoreDictionaries(List<HandlerDictionary> dictionaries) {
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.starttls.StartTLSStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.sasl.SASLStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.bind.BindResourceDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.session.SessionStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.JabberIQAuthDictionary());
    }

    public void stop() {
        serverRuntimeContext.getServerConnectorRegistry().close();
    }

	public XMPPSASLDomain addModule(Module module) {
		if(serverRuntimeContext!=null) {
			throw new IllegalStateException("Domain already started. Cannot add module "+module.getName());
		}
		modules.add(module);
		return this;
	}
	
    public XMPPSASLDomain setSASLMechanisms(SASLMechanism...validMechanisms) {
        saslMechanisms.addAll(Arrays.asList(validMechanisms));
        return this;
    }
    
    public XMPPSASLDomain setTLSCertificateInfo(File certificate, String password) throws FileNotFoundException {
        tlsCertificate = new FileInputStream(certificate);
        tlsCertificatePassword = password;
        return this;
    }

    public XMPPSASLDomain setTLSCertificateInfo(InputStream certificate, String password) {
        tlsCertificate = certificate;
        tlsCertificatePassword = password;
        return this;
    }
    
    public XMPPSASLDomain loadDefaultModules() {
        addModule(new SoftwareVersionModule());
        addModule(new EntityTimeModule());
//        domain.addModule(new VcardTempModule());
        addModule(new XmppPingModule());
//        domain.addModule(new PrivateDataModule());
        return this;
    }
}
