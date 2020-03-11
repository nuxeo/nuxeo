/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.io.IOException;
import java.util.List;

import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.persistence.fs.FileSystemStorage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ContributionPersistenceComponent extends DefaultComponent implements ContributionPersistenceManager {

    public static final String STORAGE_XP = "storage";

    protected ContributionStorage storage;

    protected RuntimeContext ctx;

    public static String getComponentName(String contribName) {
        return "config:" + contribName;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        this.ctx = context.getRuntimeContext();
        storage = new FileSystemStorage();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        ctx = null;
        storage = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // This extension point is a singleton. It supports only one
        // contribution!
        // I am not using a runtime property to specify the implementation class
        // because
        // of possible problems caused by class loaders in real OSGI frameworks.
        ContributionStorageDescriptor c = (ContributionStorageDescriptor) contribution;
        try {
            storage = (ContributionStorage) c.clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        storage = null;
    }

    @Override
    public List<Contribution> getContributions() {
        return storage.getContributions();
    }

    @Override
    public Contribution getContribution(String name) {
        return storage.getContribution(name);
    }

    @Override
    public Contribution addContribution(Contribution contrib) {
        return storage.addContribution(contrib);
    }

    @Override
    public boolean removeContribution(Contribution contrib) {
        return storage.removeContribution(contrib);
    }

    @Override
    public boolean isInstalled(Contribution contrib) {
        return ctx.isDeployed(contrib);
    }

    @Override
    public synchronized boolean installContribution(Contribution contrib) {
        RegistrationInfo ri;
        try {
            ri = ctx.deploy(contrib);
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
        if (ri == null) {
            return false;
        }
        ri.setPersistent(true);
        return true;
    }

    @Override
    public boolean uninstallContribution(Contribution contrib) {
        boolean ret = isInstalled(contrib);
        try {
            ctx.undeploy(contrib);
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
        return ret;
    }

    @Override
    public Contribution updateContribution(Contribution contribution) {
        return storage.updateContribution(contribution);
    }

    @Override
    public boolean isPersisted(Contribution contrib) {
        return storage.getContribution(contrib.getName()) != null;
    }

    @Override
    public void start() {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                installContribution(c);
            }
        }
    }

    @Override
    public void stop() {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                uninstallContribution(c);
            }
        }
    }

    @Override
    public void start(ComponentContext context) {
        if (storage == null) {
            storage = new FileSystemStorage();
            start();
        }
    }

    @Override
    public void stop(ComponentContext context) {
        stop();
    }

}
