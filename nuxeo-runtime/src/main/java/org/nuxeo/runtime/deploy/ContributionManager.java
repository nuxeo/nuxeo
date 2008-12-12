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

package org.nuxeo.runtime.deploy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionManager extends DependencyTree<String, Contribution> {

    private static final Log log = LogFactory.getLog(ContributionManager.class);

    private final ManagedComponent component;

    public ContributionManager(ManagedComponent component) {
        this.component = component;
    }

    public ManagedComponent getComponent() {
        return component;
    }

    public void registerContribution(Contribution contrib) {
        String baseId = null;
        if (contrib instanceof ExtensibleContribution) {
            ExtensibleContribution c = (ExtensibleContribution)contrib;
            baseId = c.getBaseContributionId();
        }
        Collection<String> deps = new ArrayList<String>();
        if (baseId != null) {
            deps.add(baseId);
        }
        Collection<String> cdeps = contrib.getDependencies();
        if (cdeps != null) {
            deps.addAll(cdeps);
        }
        add(contrib.getContributionId(), contrib, deps);
    }

    public void unregisterContribution(Contribution contrib) {
        remove(contrib.getContributionId());
    }

    @Override
    protected void resolved(Entry<String, Contribution> entry) {
        Contribution contrib = entry.get();
        contrib.resolve(this);
        try {
            contrib.install(component);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    protected void unresolved(Entry<String, Contribution> entry) {
        Contribution contrib = entry.get();
        try {
            contrib.uninstall(component);
        } catch (Exception e) {
            log.error(e);
        }
        contrib.unresolve(this);
    }

}
