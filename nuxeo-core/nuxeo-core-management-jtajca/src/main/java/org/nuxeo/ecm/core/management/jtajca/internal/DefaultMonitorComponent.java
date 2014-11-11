/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component used to install/uninstall the monitors (transaction and
 * connections).
 *
 * @since 5.6
 */
public class DefaultMonitorComponent extends DefaultComponent {

    protected DefaultCoreSessionMonitor smon;

    protected DefaultTransactionMonitor tmon;

    protected Map<String, DefaultConnectionMonitor> cmons = new HashMap<String, DefaultConnectionMonitor>();

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
    }

    // don't use activate, it would be too early
    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        uninstall();
        install();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        uninstall();
        super.deactivate(context);
    }

    protected boolean installed;

    protected void install() throws Exception {
        installed = true;
        smon = new DefaultCoreSessionMonitor();
        smon.install();
        tmon = new DefaultTransactionMonitor();
        tmon.install();
        RepositoryService repositoryService = NXCore.getRepositoryService();
        RepositoryManager repositoryManager = repositoryService.getRepositoryManager();
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            activateRepository(repositoryName);
            DefaultConnectionMonitor cmon = new DefaultConnectionMonitor(
                    repositoryName);
            cmon.install();
            cmons.put(repositoryName, cmon);
        }
    }

    /**
     * Make sure we open the repository, to initialize its connection manager.
     */
    protected void activateRepository(String repositoryName)
            throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", SecurityConstants.SYSTEM_USERNAME);
        CoreSession session = CoreInstance.getInstance().open(repositoryName,
                context);
        CoreInstance.getInstance().close(session);
    }

    protected void uninstall() throws JMException {
        if (!installed) {
            return;
        }
        installed = false;
        for (DefaultConnectionMonitor cmon : cmons.values()) {
            cmon.uninstall();
        }
        smon.uninstall();
        tmon.uninstall();
        cmons.clear();
        smon = null;
        tmon = null;
    }



    protected static ObjectInstance bind(Object managed) {
        return bind(managed, "jdoe");
    }

    protected static ObjectInstance bind(Class<?> itf, Object managed) {
        return bind(itf, managed, "jdoe");
    }

    protected static ObjectInstance bind(Object managed, String name) {
        return bind(managed.getClass().getInterfaces()[0], managed, name);
    }

    protected static ObjectInstance bind(Class<?> itf, Object managed, String name) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        name = Defaults.instance.name(itf, name);
        try {
            return mbs.registerMBean(managed, new ObjectName(name));
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot bind " + managed + " on " + name, e);
        }
    }

    protected static void unbind(ObjectInstance instance) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(instance.getObjectName());
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot unbind " + instance, e);
        }
    }
}
