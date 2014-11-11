/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.persistence.fs.FileSystemStorage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionPersistenceComponent extends DefaultComponent
        implements ContributionPersistenceManager {

    private static final Log log = LogFactory.getLog(ContributionPersistenceComponent.class);

    public static final String STORAGE_XP = "storage";

    protected ContributionStorage storage;

    protected RuntimeContext ctx;

    public static String getComponentName(String contribName) {
        return "config:" + contribName;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        ctx = context.getRuntimeContext();
        storage = new FileSystemStorage();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        ctx = null;
        storage = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // This extension point is a singleton. It supports only one
        // contribution!
        // I am not using a runtime property to specify the implementation class
        // because
        // of possible problems caused by class loaders in real OSGI frameworks.
        ContributionStorageDescriptor c = (ContributionStorageDescriptor) contribution;
        storage = (ContributionStorage) c.clazz.newInstance();
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        storage = null;
    }

    @Override
    public List<Contribution> getContributions() throws Exception {
        return storage.getContributions();
    }

    @Override
    public Contribution getContribution(String name) throws Exception {
        return storage.getContribution(name);
    }

    @Override
    public Contribution addContribution(Contribution contrib) throws Exception {
        return storage.addContribution(contrib);
    }

    @Override
    public boolean removeContribution(Contribution contrib) throws Exception {
        return storage.removeContribution(contrib);
    }

    @Override
    public boolean isInstalled(Contribution contrib) throws Exception {
        return ctx.isDeployed(contrib);
    }

    @Override
    public synchronized boolean installContribution(Contribution contrib)
            throws Exception {
        RegistrationInfo[] ris = ctx.deploy(contrib);
        for (RegistrationInfo ri:ris) {
            ri.setPersistent(true);
        }
        return ris.length > 0;
    }

    @Override
    public boolean uninstallContribution(Contribution contrib) throws Exception {
        boolean ret = isInstalled(contrib);
        ctx.undeploy(contrib);
        return ret;
    }

    @Override
    public Contribution updateContribution(Contribution contribution)
            throws Exception {
        return storage.updateContribution(contribution);
    }

    @Override
    public boolean isPersisted(Contribution contrib) throws Exception {
        return storage.getContribution(contrib.getName()) != null;
    }

    @Override
    public void start() throws Exception {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                installContribution(c);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                uninstallContribution(c);
            }
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (storage == null) {
            storage = new FileSystemStorage();
            try {
                start();
            } catch (Exception e) {
                log.error("Failed to start contribution persistence service", e);
            }
        }
    }
}
