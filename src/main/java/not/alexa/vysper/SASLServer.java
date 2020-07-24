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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.mina.MultiHostEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContextProvider;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

public class SASLServer {
    private final List<MultiHostEndpoint> endpoints = new ArrayList<MultiHostEndpoint>();
    private final Map<String,XMPPSASLDomain> domains=new HashMap<>();
    
    public SASLServer() {
    }
    
    public synchronized XMPPSASLDomain createDomain(String serverDomain) {
    	XMPPSASLDomain context=domains.get(serverDomain);
    	if(context==null) {
    		context=new XMPPSASLDomain(serverDomain);
    		domains.put(serverDomain, context);
    	}
    	return context;
    }

    public void addEndpoint(Endpoint endpoint) {
    	if(endpoint instanceof MultiHostEndpoint) {
    		endpoints.add((MultiHostEndpoint)endpoint);
    	} else {
    		throw new IllegalArgumentException(getClass().getSimpleName()+" supports multi host endpoints only.");
    	}
    }
    
    public void start(StorageProviderRegistry storageProviderRegistry,ResourceRegistry resourceRegistry) throws IOException {
    	ServerRuntimeContextProvider contextProvider=new ServerRuntimeContextProvider();
        for(Map.Entry<String,XMPPSASLDomain> domain:domains.entrySet()) {
        	contextProvider.add(domain.getValue().start(contextProvider,storageProviderRegistry, resourceRegistry));
        }
        if (endpoints.size() == 0) {
            throw new IllegalStateException("server must have at least one endpoint");
        }
        for (MultiHostEndpoint endpoint : endpoints) {
       		endpoint.start(contextProvider);
        }
    }

    public void stop() {
        for (Endpoint endpoint : endpoints) {
            endpoint.stop();
        }
        for(XMPPSASLDomain domain:domains.values()) {
        	domain.stop();
        }
    }
}
