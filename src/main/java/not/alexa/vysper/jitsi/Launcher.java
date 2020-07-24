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
package not.alexa.vysper.jitsi;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.Anonymous;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.extension.xep0114.ExternalComponent;
import org.apache.vysper.xmpp.extension.xep0114.ExternalComponentModule;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryOccupantStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.InMemoryRoomStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule;

import not.alexa.vysper.MultiHostXMPPServer;
import not.alexa.vysper.XMPPSASLDomain;

/**
 * The class launches a XMPP server suitable configured for Jitsi as described in the
 * <a href="https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-manual">Jitsi Manual Installation Guide</a>.
 * Especially the variable names are taken from that source (and the values needs to be adjusted of course).
 * <br>The security data needed to establish TLS connections is handled differently compared with the Prosody server. Java uses in general
 * keystores for this purpose which has to be created out of the key material generated in the above mentioned guide. This is described
 * in the general README of this project. The keystores used by this launcher should be <code>&lt;domain&gt;.p12</code> resp. <code>auth.&lt;domain&gt;.p12</code>
 * and should be available on the classpath.  
 *  
 * @author notalexa
 *
 */
public class Launcher {
	private static final String YOURSECRET1="secret1";
	private static final String YOURSECRET2="secret2";
	private static final String YOURSECRET3="secret3";
	private static final String JITSI_DEFAULT_DOMAIN="jitsi.example.com";
	private static final String KEYSTORE_JITSI_DOMAIN_SECRET="secret";
	private static final String KEYSTORE_AUTH_DOMAIN_SECRET="secret";
	
	private static final int XMPP_PORT=5222;
	private static final int BOSH_PORT=5280;
	private static final int EXTERNAL_COMPONENT_PORT=5347;
	
    public static void main(String[] args) throws Exception {
    	String domain=JITSI_DEFAULT_DOMAIN;
    	if(args.length>0) {
    		domain=args[0];
    	}
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        RoomStorageProvider roomStorageProvider=new InMemoryRoomStorageProvider();
        providerRegistry.add(roomStorageProvider);
        providerRegistry.add(new InMemoryOccupantStorageProvider());
        AccountManagement accountManagement = (AccountManagement) providerRegistry.retrieve(AccountManagement.class);
        accountManagement.addUser(EntityImpl.parseUnchecked("focus@auth."+domain), YOURSECRET3);

        MultiHostXMPPServer server = new MultiHostXMPPServer(providerRegistry);
        XMPPSASLDomain domainContext1=server.createSASLDomain(domain,new Anonymous())
        		.setTLSCertificateInfo(resolve(domain+".p12"),KEYSTORE_JITSI_DOMAIN_SECRET)
        		.loadDefaultModules();
        ExternalComponentModule external=new ExternalComponentModule();
        external.addComponent(new ExternalComponent("jitsi-videobridge", YOURSECRET1));
        external.addComponent(new ExternalComponent("focus", YOURSECRET2));
        domainContext1.addModule(external);
        domainContext1.addModule(new MUCModule("conference"));
        domainContext1.addModule(new PublishSubscribeModule());
        /*XMPPSASLDomain domainContext2=*/server.createSASLDomain("auth."+domain,new Plain())
        		.setTLSCertificateInfo(resolve("auth."+domain+".p12"), KEYSTORE_AUTH_DOMAIN_SECRET)
        		.loadDefaultModules();
        server.addEndpoint(new org.apache.vysper.mina.TCPEndpoint(XMPP_PORT));
        server.addEndpoint(external.createEndPoint(EXTERNAL_COMPONENT_PORT));
        server.addEndpoint(new BoshEndpoint(BOSH_PORT).setContextPath("/http-bind"));
        server.start();
        System.out.println("vysper server is running...");
    }
    
    private static InputStream resolve(String name) throws FileNotFoundException {
    	InputStream stream=Launcher.class.getResourceAsStream("/"+name);
    	if(stream==null) {
    		throw new FileNotFoundException(name);
    	}
    	return stream;
    }
}
