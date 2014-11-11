/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.config;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.remoting.RemotingService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NodeConfiguration implements Serializable, Cloneable {

    public static final String NODE_ID = "org.nuxeo.runtime.nodeId";

    public static final String SERVER_ENABLED = "org.nuxeo.runtime.server.enabled";
    public static final String SERVER_TYPE = "org.nuxeo.runtime.server.type";
    public static final String SERVER_LOCATOR = "org.nuxeo.runtime.server.locator";
    public static final String SERVER_HOST = "org.nuxeo.runtime.server.host";
    public static final String SERVER_PORT = "org.nuxeo.runtime.server.port";

    public static final String PEERS = "org.nuxeo.runtime.peers";
    public static final String AUTODETECT_PEERS = "org.nuxeo.runtime.peers.autodetect";
    public static final String CLIENT_JNDI_PREFIX = "nuxeo-client-jndi.";

    public static final String PRODUCT_NAME = "org.nuxeo.ecm.product.name";
    public static final String PRODUCT_VERSION = "org.nuxeo.ecm.product.version";

    public static final String IS_STREAMING_SERVER = "org.nuxeo.runtime.streaming.isServer";
    public static final String STREAMING_LOCATOR = "org.nuxeo.runtime.streaming.serverLocator";

    private static NodeConfiguration configuration;

    private static final long serialVersionUID = 1227680972931266947L;

    private final String nodeId;
    private final String serverType;

    private InvokerLocator locator;
    private boolean isServer = false;
    //private ServerInstance[] peers;
    private Properties env;

    private final String productName;
    private final String productVersion;

    private String streamingLocator;
    private boolean isStreamingServer;
    //TODO
    //private String resourceLocator;

    private boolean isAutoDetectingPeers = false;


    public static NodeConfiguration getConfiguration() {
        if (configuration == null) {
            try {
                configuration = new NodeConfiguration();
            } catch (MalformedURLException e) {
                throw new Error("Faiuled to initialize Node Configuration", e);
            }
        }
        return configuration;
    }

    /**
     * Gives the possibility to override the current configuration.
     * This is useful for clients using auto configuration like Apogee.
     * @param cfg the configuration to use as the current node configuration
     */
    public static void setConfiguration(NodeConfiguration cfg) {
        configuration = cfg;
    }

    public NodeConfiguration() throws MalformedURLException {
        nodeId = Framework.getProperty(NODE_ID, "local");
        isServer = Boolean.parseBoolean(Framework.getProperty(SERVER_ENABLED, "false"));
        serverType = Framework.getProperty(SERVER_TYPE);
        String uri = Framework.getProperty(SERVER_LOCATOR);
        if (uri == null) {
            String host = Framework.getProperty(SERVER_HOST, "localhost");
            int port = Integer.parseInt(Framework.getProperty(SERVER_PORT, "62474"));
            uri = "socket://"+host+":"+port+"/?datatype=nuxeo";
        }
        locator = new InvokerLocator(uri);
        productName = Framework.getProperty(PRODUCT_NAME);
        productVersion = Framework.getProperty(PRODUCT_VERSION);
        isStreamingServer = Boolean.parseBoolean(Framework.getProperty(IS_STREAMING_SERVER, "false"));
        if (isStreamingServer) {
            streamingLocator = Framework.getLocalService(RemotingService.class).getServerLocator().getLocatorURI();
        } else {
            streamingLocator = Framework.getProperty(STREAMING_LOCATOR);
        }
        isAutoDetectingPeers = Boolean.parseBoolean(Framework.getProperty(AUTODETECT_PEERS, "false"));
        // peers are loaded lazy
        // load jndi client env
        env = new Properties();
        int len = CLIENT_JNDI_PREFIX.length();
        for (Map.Entry<Object,Object> entry : Framework.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(CLIENT_JNDI_PREFIX)) {
                env.put(key.substring(len), entry.getValue());
            }
        }
        if (env.isEmpty()) {
            env = null;
        }
    }

    //TODO XXX should find something else. Cannot rely on JBoss variables  ...
    public boolean isServerNode() {
        return System.getProperty("jboss.home.dir") != null;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isServer() {
        return isServer;
    }

    public String getServerType() {
        return serverType;
    }

    public InvokerLocator getLocator() {
        return locator;
    }

    public String getHost() {
        return locator.getHost();
    }

    public int getPort() {
        return locator.getPort();
    }

//    /**
//     * @return the peers.
//     */
//    public ServerInstance[] getPeers() throws MalformedURLException {
//        if (peers == null) {
//            String peersSpec = Framework.getProperty(PEERS, "");
//            if (peersSpec.length() >0) {
//                // parse peers
//                String[] peersAr = StringUtils.split(peersSpec, ',', true);
//                // resolve aliases
//                for (int i=0; i<peersAr.length; i++) {
//                    String peer = peersAr[i];
//                    int p = peer.indexOf("://");
//                    if (p > -1) {
//                        peers[i] = new ServerInstance(peer);
//                    } else {
//                        p = peer.indexOf(':');
//                        if (p > -1) {
//                            peers[i] = new ServerInstance(peer.substring(0, p), Integer.parseInt(peer.substring(p+1)));
//                        } else {
//                            peers[i] = new ServerInstance(peer, LocatorHelper.DEFAULT_PORT);
//                        }
//                    }
//                }
//            } else {
//                peers = new ServerInstance[0];
//            }
//        }
//        return peers;
//    }

    public String getProductName() {
        return productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public String getStreamingLocator() {
        return streamingLocator;
    }

    public boolean isStreamingServer() {
        return isStreamingServer;
    }

    public boolean isAutoDetectingPeers() {
        return isAutoDetectingPeers;
    }

    public Properties getClientEnvironment() {
        return env;
    }

    /**
     * Normalize hosts specifications from several config properties like the invoker, streaming server, jndi client env.
     * given a final host as used by the client at connection time.
     * <p>
     * Should be called by clients using the locator of the remote server after downoalding the configuration from the server.
     * @param locator
     */
    public void normalize(InvokerLocator locator) throws MalformedURLException {
        // normalize server locator
        if (isServer && !locator.equals(this.locator)) {
            String host = ConfigurationHelper.getNormalizedHost(this.locator.getHost(), locator.getHost());
            if (!host.equals(this.locator.getHost())) {
                this.locator = new InvokerLocator(ConfigurationHelper.getNormalizedURI
                        (this.locator.getLocatorURI(), locator.getHost()));
            }
        }
        // normalize streaming server locator
        if (isStreamingServer) {
            streamingLocator = locator.getLocatorURI();
        } else if (streamingLocator != null) {
            streamingLocator = ConfigurationHelper.getNormalizedURI(streamingLocator, locator.getHost());
        }
        // normalize client env
        if (env != null) {
            String value = env.getProperty(Context.PROVIDER_URL);
            if (value != null) {
                value = String.format(value, locator.getHost());
                env.put(Context.PROVIDER_URL, value);
            }
        }
    }

    @Override
    public NodeConfiguration clone() throws CloneNotSupportedException {
        NodeConfiguration clone = (NodeConfiguration)super.clone();
        clone.env = new Properties();
        clone.env.putAll(env);
        return clone;
    }

}
