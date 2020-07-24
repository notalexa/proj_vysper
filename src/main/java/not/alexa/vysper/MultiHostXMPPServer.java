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

import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 * 
 * @author notalexa
 *
 */
public class MultiHostXMPPServer {
	private SASLServer server;
    private StorageProviderRegistry storageProviderRegistry;
    
    public MultiHostXMPPServer(StorageProviderRegistry storageProviderRegistry) {
    	server=new SASLServer();
    	this.storageProviderRegistry=storageProviderRegistry;
    }
    
    public XMPPSASLDomain createSASLDomain(String domain,SASLMechanism...validMechanisms) {
    	XMPPSASLDomain saslDomain=server.createDomain(domain);
    	saslDomain.setSASLMechanisms(validMechanisms);
    	return saslDomain;
    }

    public void addEndpoint(Endpoint endpoint) {
        server.addEndpoint(endpoint);
    }

    public void start() throws Exception {
        ResourceRegistry resourceRegistry = new ResourceRegistry();
        server.start(storageProviderRegistry, resourceRegistry);
    }
    
    public void stop() {
    	server.stop();
    }
    
    public SASLServer getServer() {
    	return server;
    }
}
