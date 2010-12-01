/*
 * (C) Copyright 2007-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.services.streaming;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.remoting.RemotingService;
import org.nuxeo.runtime.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StreamingService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(StreamingService.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.streaming");

    protected StreamManager manager;

    protected ComponentContext context;

    protected boolean isServer;

    protected String serverLocator;

    @Override
    public void activate(ComponentContext context) throws Exception {
        this.context = context;
        String val = Framework.getProperty(
                "org.nuxeo.runtime.streaming.isServer", "true");
        isServer = val.equalsIgnoreCase("true");
        serverLocator = Framework.getProperty("org.nuxeo.runtime.streaming.serverLocator");
        boolean isServerEnabled = Framework.getProperty(
                "org.nuxeo.runtime.server.enabled", "true").equalsIgnoreCase(
                "true");
        if (isServerEnabled) {
            startManager();
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // stop the manager
        stopManager();
        this.context = null;
        super.deactivate(context);
    }

    public void setServer(boolean isServer) {
        this.isServer = isServer;
    }

    public void setServerLocator(String serverLocator) {
        this.serverLocator = serverLocator;
    }

    public boolean isServer() {
        return isServer;
    }

    public String getServerLocator() {
        return serverLocator;
    }

    public ComponentContext getContext() {
        return context;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
    }

    public StreamManager getStreamManager() {
        return manager;
    }

    public synchronized void startManager() throws Exception {
        if (manager != null) {
            throw new IllegalStateException(
                    "StreamingManager is already started");
        }

        if (isServer) {
            File tmpDir = new File(Framework.getRuntime().getHome(),
                    "tmp/uploads");
            RemotingService remoting = (RemotingService) Framework.getRuntime().getComponent(
                    RemotingService.NAME);
            if (remoting == null) {
                // TODO throw other exception!
                throw new IllegalStateException(
                        "Cannot start manager. RemotingService not available.");
            }
            TransporterServer transporterServer = remoting.getTransporterServer();
            if (transporterServer != null) {
                manager = new StreamManagerServer(transporterServer, tmpDir);
                serverLocator = transporterServer.getLocatorURI();
            } else {
                String msg = "Streaming Transporter Server is not defined. Streaming will not work.";
                log.warn(msg);
                Framework.getRuntime().getWarnings().add(msg);
            }
        } else if (serverLocator == null) {
            String msg = "Streaming Server Locator is not defined. Streaming will not work.";
            log.warn(msg);
            Framework.getRuntime().getWarnings().add(msg);
        } else {
            int minBufSize = (Integer) context.getPropertyValue(
                    "minBufferSize", 1024 * 8);
            int maxBufSize = (Integer) context.getPropertyValue(
                    "maxBufferSize", 1024 * 1024 * 8);
            manager = new StreamManagerClient(serverLocator, minBufSize,
                    maxBufSize);
        }
        if (manager != null) {
            manager.start();
        }
    }

    public synchronized void stopManager() throws Exception {
        if (manager != null) {
            manager.stop();
            manager = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (StreamManager.class.isAssignableFrom(adapter)) {
            return (T) manager;
        }
        return null;
    }

}
