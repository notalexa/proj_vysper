package org.apache.vysper.xmpp.extension.xep0114;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * An endpoint for external components.
 * 
 * @author notalexa
 *
 */
public class ExternalComponentEndpoint extends TCPEndpoint {

	public ExternalComponentEndpoint(int port) {
		setPort(port);
	}

	@Override
	public boolean isContextAllowed(ServerRuntimeContext serverRuntimeContext) {
		return ExternalComponentContext.class.isInstance(serverRuntimeContext);
	}
}
