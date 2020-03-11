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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ManagedComponent extends DefaultComponent {

    protected static final Log log = LogFactory.getLog(ManagedComponent.class);

    protected Map<String, ContributionManager> contributionManagers;

    @Override
    public void activate(ComponentContext context) {
        contributionManagers = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        contributionManagers = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof Contribution) {
                Contribution c = (Contribution) contrib;
                c.setExtension(extension);
                ContributionManager mgr = contributionManagers.get(c.getExtensionPoint());
                if (mgr != null) {
                    mgr.registerContribution(c);
                } else {
                    log.warn("Unable to register contribution: " + c.getContributionId() + " for extension point "
                            + c.getExtensionPoint() + ". No manager registered.");
                }
            } else {
                registerContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof Contribution) {
                Contribution c = (Contribution) contrib;
                c.setExtension(extension);
                ContributionManager mgr = contributionManagers.get(c.getExtensionPoint());
                if (mgr != null) {
                    mgr.unregisterContribution(c);
                } else {
                    log.warn("Unable to unregister contribution: " + c.getContributionId() + " for extension point "
                            + c.getExtensionPoint() + ". No manager registered.");
                }
            } else {
                unregisterContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
            }
        }
    }

    public void registerContributionManager(String extensionPoint, ContributionManager mgr) {
        contributionManagers.put(extensionPoint, mgr);
    }

}
