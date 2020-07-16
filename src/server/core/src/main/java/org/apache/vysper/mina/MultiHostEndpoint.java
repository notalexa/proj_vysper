package org.apache.vysper.mina;

import java.io.IOException;

import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContextProvider;

/**
 * An endpoint able to handle multiple domains (and components therein).
 * 
 * @author notalexa
 *
 */
public interface MultiHostEndpoint extends Endpoint {
	
	/**
	 * Start this endpoint using the given context provider.
	 * 
	 * @param contextProvider the provider managing the domains
	 * @throws IOException if an error occurs
	 */
	public void start(ServerRuntimeContextProvider contextProvider) throws IOException;

    /**
     * is the resolved context allowed for this handler
     * @param context the context to check
     * @return <code>if the context can is allowed for this endpoint
     */

	public default boolean isContextAllowed(ServerRuntimeContext serverRuntimeContext) {
		return serverRuntimeContext.isXmppDomain();
	}
}
