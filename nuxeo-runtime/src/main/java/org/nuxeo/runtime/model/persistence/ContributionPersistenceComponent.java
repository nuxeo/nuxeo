/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime.model.persistence;

import java.util.List;

import org.nuxeo.runtime.model.ComponentContext;
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

    public static final String P_STORAGE = "org.nxueo.runtime.model.persistence.ContributionStorage";

    protected ContributionStorage storage;

    protected RuntimeContext ctx;

    public static String getComponentName(String contribName) {
        return "config:" + contribName;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        this.ctx = context.getRuntimeContext();
        storage = new FileSystemStorage();
        // TODO add extension point to be able to configure the storage.
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        ctx = null;
        storage = null;
    }

    public List<Contribution> getContributions() throws Exception {
        return storage.getContributions();
    }

    public Contribution getContribution(String name) throws Exception {
        return storage.getContribution(name);
    }

    public Contribution addContribution(Contribution contrib) throws Exception {
        return storage.addContribution(contrib);
    }

    public Contribution addAndInstallContribution(Contribution contrib)
            throws Exception {
        Contribution c = storage.addContribution(contrib);
        if (c == null) {
            return null;
        }
        installContribution(contrib);
        return c;
    }

    public boolean removeContribution(Contribution contrib) throws Exception {
        return storage.removeContribution(contrib);
    }

    public boolean removeAndUninstallContribution(Contribution contrib)
            throws Exception {
        uninstallContribution(contrib);
        removeContribution(contrib);
        return true;
    }

    public boolean isInstalled(Contribution contrib) throws Exception {
        return ctx.isDeployed(contrib);
    }

    public synchronized boolean installContribution(Contribution contrib)
            throws Exception {
        RegistrationInfo ri = ctx.deploy(contrib);
        if (ri == null) {
            return false;
        }
        ri.setPersistent(true);
        return true;
    }

    public boolean uninstallContribution(Contribution contrib) throws Exception {
        ctx.undeploy(contrib);
        return true;
    }

    public Contribution updateContribution(Contribution contribution)
            throws Exception {
        return storage.updateContribution(contribution);
    }

    public boolean isPersisted(String name) throws Exception {
        return storage.getContribution(name) != null;
    }

    public void start() throws Exception {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                installContribution(c);
            }
        }
    }

    public void stop() throws Exception {
        for (Contribution c : storage.getContributions()) {
            if (!c.isDisabled()) {
                uninstallContribution(c);
            }
        }
    }

}
