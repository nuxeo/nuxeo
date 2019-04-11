/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
            ExtensibleContribution c = (ExtensibleContribution) contrib;
            baseId = c.getBaseContributionId();
        }
        Collection<String> deps = new ArrayList<>();
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
        } catch (RuntimeException e) {
            log.error(e, e);
        }
    }

    @Override
    protected void unresolved(Entry<String, Contribution> entry) {
        Contribution contrib = entry.get();
        try {
            contrib.uninstall(component);
        } catch (RuntimeException e) {
            log.error(e, e);
        }
        contrib.unresolve(this);
    }

}
