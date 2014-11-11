/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.client;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginException;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.config.AutoConfigurationService;
import org.nuxeo.runtime.remoting.RemotingService;
import org.nuxeo.runtime.services.streaming.StreamingService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class NuxeoClient {

    private LoginHandler loginHandler;
    private final List<RepositoryInstance> repositoryInstances;
    private final ListenerList connectionListeners;

    private InvokerLocator locator;
    private String serverName;
    private final AutoConfigurationService cfg;
    private RepositoryManager repositoryMgr = null;

    private boolean multiThreadedLogin = false;

    private static final NuxeoClient instance = new NuxeoClient();

    private NuxeoClient() {
        this.connectionListeners = new ListenerList();
        this.cfg = new AutoConfigurationService();
        this.loginHandler = loginHandler == null ? new DefaultLoginHandler() : loginHandler;
        this.repositoryInstances = new Vector<RepositoryInstance>();
    }

    public static NuxeoClient getInstance() {
        return instance;
    }

    public void setMultiThreadedLogin(boolean useMultiThreadedLogin) {
        this.multiThreadedLogin = useMultiThreadedLogin;
    }

    public boolean getMultiThreadedLogin() {
        return multiThreadedLogin;
    }

    public synchronized  void connect(String locator) throws Exception {
        if (this.locator != null) {
            throw new IllegalStateException("Client is already connected");
        }
        doConnect(AutoConfigurationService.createLocator(locator));
    }

    public synchronized  void connect(InvokerLocator locator) throws Exception {
        if (this.locator != null) {
            throw new IllegalStateException("Client is already connected");
        }
        doConnect(locator);
    }

    public synchronized  void connect(String host, int port) throws Exception {
        if (this.locator != null) {
            throw new IllegalStateException("Client is already connected");
        }
        doConnect(AutoConfigurationService.createLocator(host, port));
    }

    public synchronized  void forceConnect(InvokerLocator locator) throws Exception {
        if (this.locator != null) {
            disconnect();
        }
        doConnect(locator);
    }

    public synchronized  void forceConnect(String locator) throws Exception {
        if (this.locator != null) {
            disconnect();
        }
        doConnect(AutoConfigurationService.createLocator(locator));
    }

    public synchronized  void forceConnect(String host, int port) throws Exception {
        if (this.locator != null) {
            disconnect();
        }
        doConnect(AutoConfigurationService.createLocator(host, port));
    }

    public synchronized  void tryConnect(String host, int port) throws Exception {
        if (this.locator != null) {
            return; // do nothing
        }
        doConnect(AutoConfigurationService.createLocator(host, port));
    }

    public synchronized  void tryConnect(String url) throws Exception {
        if (this.locator != null) {
            return; // do nothing
        }
        doConnect(AutoConfigurationService.createLocator(url));
    }

    public synchronized  void tryConnect(InvokerLocator locator) throws Exception {
        if (this.locator != null) {
            return; // do nothing
        }
        doConnect(locator);
    }

    private void doConnect(InvokerLocator locator) throws Exception {
        this.locator = locator;
        try {
            cfg.load(locator);
            // FIXME TODO workarounf to work with nxruntime core 1.3.3 --------------
            String newPort = Framework.getProperty("org.nuxeo.runtime.1.3.3.streaming.port");
            if (newPort != null) {
                StreamingService streamingService = (StreamingService) Framework.getRuntime().getComponent(
                    StreamingService.NAME);
                // streaming config
                String oldLocator = streamingService.getServerLocator();
                int p = oldLocator.lastIndexOf(':');
                if (p > -1) {
                    String withoutPort = oldLocator.substring(0, p);
                    String serverLocator = withoutPort+":"+newPort;
                    streamingService.stopManager();
                    streamingService.setServerLocator(serverLocator);
                    streamingService.setServer(false);
                    streamingService.startManager();
                }
            }
            // FIXME TODO workaround for remote services -------------------------------
            schemaRemotingWorkaround(locator.getHost());
            // workaround for client login configuration - we need to make it not multi threaded
            // TODO put an option for this in NuxeoClient
            if (!multiThreadedLogin) {
                LoginService ls = Framework.getService(LoginService.class);
                SecurityDomain sysDomain = ls.getSecurityDomain(LoginComponent.SYSTEM_LOGIN);
                SecurityDomain clientDomain = ls.getSecurityDomain(LoginComponent.CLIENT_LOGIN);
                adaptClientSecurityDomain(sysDomain);
                adaptClientSecurityDomain(clientDomain);
            }
            // ----------------
            login();
        } catch (Exception e) {
            this.locator = null;
            throw e;
        }
        fireConnected(this);
    }

    public void adaptClientSecurityDomain(SecurityDomain sd) {
        AppConfigurationEntry[] entries = sd.getAppConfigurationEntries();
        if (entries != null) {
            for (int i=0; i<entries.length; i++) {
                AppConfigurationEntry entry = entries[i];
                if ("org.jboss.security.ClientLoginModule".equals(entry.getLoginModuleName())) {
                    Map<String, ?> opts = entry.getOptions();
                    Map<String, Object> newOpts = new HashMap<String, Object>(opts);
                    newOpts.put("multi-threaded", "false");
                    entries[i] = new AppConfigurationEntry(entry.getLoginModuleName(),
                            entry.getControlFlag(), entry.getOptions());
                }
            }
        }
    }

    /**
     * Workaround for being able to load schemas from remote
     * TODO integrate this in core
     * FIXME integrate this in core
     */
    private void schemaRemotingWorkaround(String host) throws Exception {
        ServiceManager serviceManager = Framework.getLocalService(ServiceManager.class);
        ServiceDescriptor sd = new ServiceDescriptor(TypeProvider.class, "core");
        sd.setLocator("%TypeProviderBean");
        serviceManager.registerService(sd);
        TypeProvider typeProvider = Framework.getService(TypeProvider.class);
        SchemaManager schemaMgr = Framework.getLocalService(SchemaManager.class);
        ((SchemaManagerImpl)schemaMgr).importTypes(typeProvider);
    }

    public synchronized void disconnect() throws Exception {
        if (this.locator == null) {
            throw new IllegalStateException("Client is not connected");
        }
        doDisconnect();
    }

    public synchronized void tryDisconnect() throws Exception {
        if (this.locator == null) {
            return; // do nothing
        }
        doDisconnect();
    }

    private void doDisconnect() throws Exception {
        this.locator = null;
        this.serverName = null;
        // close repository sessions if any
        Iterator<RepositoryInstance> it = repositoryInstances.iterator();
        while (it.hasNext()) {
            RepositoryInstance repo = it.next();
            try {
                repo.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            it.remove();
        }
        // logout
        logout();
        this.repositoryMgr = null;
        fireDisconnected(this);
    }

    public synchronized  void reconnect() throws Exception {
        if (this.locator == null) {
            throw new IllegalStateException("Client is not connected");
        }
        InvokerLocator locator = this.locator;
        disconnect();
        connect(locator);
    }

    public AutoConfigurationService getConfigurationService() {
        return cfg;
    }

    public synchronized String getServerName() {
        if (this.locator == null) {
            throw new IllegalStateException("Client is not connected");
        }
        if (serverName == null) {
            if (cfg == null) { // compatibility
                serverName = RemotingService.ping(locator.getHost(), locator.getPort());
            } else {
                serverName = cfg.getServerConfiguration().getProductInfo();
            }
        }
        return serverName;
    }

    public synchronized boolean isConnected() {
        return locator != null;
    }

    public String getServerHost() {
        if (this.locator == null) {
            throw new IllegalStateException("Client is not connected");
        }
        return locator.getHost();
    }

    public int getServerPort() {
        if (this.locator == null) {
            throw new IllegalStateException("Client is not connected");
        }
        return locator.getPort();
    }

    public InvokerLocator getLocator() {
        return locator;
    }

    public synchronized LoginHandler getLoginHandler() {
        return loginHandler;
    }

    public synchronized void setLoginHandler(LoginHandler loginHandler) {
        this.loginHandler = loginHandler;
    }

    public synchronized void login() throws LoginException {
        if (loginHandler != null) {
            loginHandler.login();
        }
    }

    public synchronized void logout() throws LoginException {
        if (loginHandler != null) {
            loginHandler.logout();
        }
    }

    public RepositoryManager  getRepositoryManager() throws Exception {
        if (repositoryMgr == null) {
            repositoryMgr = Framework.getService(RepositoryManager.class);
        }
        return repositoryMgr;
    }

    /**
     * Gets the repositories available on the connected server.
     *
     * @return the repositories
     */
    public Repository[] getRepositories() throws Exception {
        Collection<Repository> repos = getRepositoryManager().getRepositories();
        return repos.toArray(new Repository[repos.size()]);
    }

    public Repository getDefaultRepository() throws Exception {
        return getRepositoryManager().getDefaultRepository();
    }

    public Repository getRepository(String name) throws Exception {
        return getRepositoryManager().getRepository(name);
    }

    public RepositoryInstance openRepository() throws Exception {
        Repository repository = getRepositoryManager().getDefaultRepository();
        RepositoryInstance repo = newRepositoryInstance(repository);
        repositoryInstances.add(repo);
        return repo;
    }

    public RepositoryInstance openRepository(String name) throws Exception {
        Repository repository = getRepositoryManager().getRepository(name);
        RepositoryInstance repo = newRepositoryInstance(repository);
        repositoryInstances.add(repo);
        return repo;
    }

    public void releaseRepository(RepositoryInstance repo) throws Exception {
        try {
            repo.close();
        } finally {
            repositoryInstances.remove(repo);
        }
    }

    public RepositoryInstance[] getRepositoryInstances() {
        return repositoryInstances.toArray(new RepositoryInstance[repositoryInstances.size()]);
    }

    public static RepositoryInstance newRepositoryInstance(Repository repository) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = NuxeoClient.class.getClassLoader();
        }
        return (RepositoryInstance)Proxy.newProxyInstance(cl,
                new Class[] { RepositoryInstance.class },
                new RepositoryInstanceHandler(repository));
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    private void fireDisconnected(NuxeoClient client) {
        Object[] listeners = connectionListeners.getListeners();
        for (Object listener : listeners) {
            ((ConnectionListener)listener).disconnected(client);
        }
    }

    private void fireConnected(NuxeoClient client) {
        Object[] listeners = connectionListeners.getListeners();
        for (Object listener : listeners) {
            ((ConnectionListener)listener).connected(client);
        }
    }

}
