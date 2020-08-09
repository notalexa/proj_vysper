package org.apache.vysper.xmpp.state.resourcebinding;

import org.apache.vysper.xmpp.server.SessionContext;

public interface SessionBindListener {
	/** 
	 * Called, whenever a resource is bound to a session.
	 * @param resource the resource id 
	 * @param sessionContext the session context
	 */
	public default void onSessionBound(String resource,SessionContext sessionContext) {
	}

	/** 
	 * Called, whenever a resource is removed from a session.
	 * @param resource the resource id 
	 * @param sessionContext the session context
	 */
	public default void onSessionUnbound(String resource,SessionContext sessionContext) {
	}

}
