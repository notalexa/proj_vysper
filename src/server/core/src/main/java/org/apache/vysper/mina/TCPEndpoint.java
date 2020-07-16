package org.apache.vysper.mina;


import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.vysper.mina.StanzaLoggingFilter;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContextProvider;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TCPEndpoint implements MultiHostEndpoint {
    private int port = 5222;
    private SocketAcceptor acceptor;
    private ServerRuntimeContext serverRuntimeContext;
    private DefaultIoFilterChainBuilder filterChainBuilder;
    
    public TCPEndpoint() {
    }
    
    public TCPEndpoint(int port) {
    	this.port=port;
    }
    
    public DefaultIoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public void start() throws IOException {
    	start(new ServerRuntimeContextProvider(serverRuntimeContext));
    }
    
    public void start(IoHandler adapter) throws IOException {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        //filterChainBuilder.addLast("executorFilter", new OrderedThreadPoolExecutor());
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        acceptor.setFilterChainBuilder(filterChainBuilder);
        acceptor.setHandler(adapter);

        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(port));

        this.acceptor = acceptor;
    }

    public void stop() {
        acceptor.unbind();
        acceptor.dispose();
    }

	@Override
	public void start(ServerRuntimeContextProvider contextProvider) throws IOException {
		start(new XmppIoHandlerAdapter(this,contextProvider));
	}
}
