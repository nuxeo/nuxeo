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
 *
 */
public class ManagedComponent extends DefaultComponent {

    protected static final Log log = LogFactory.getLog(ManagedComponent.class);

    protected Map<String, ContributionManager> contributionManagers;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contributionManagers = new HashMap<String, ContributionManager>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        contributionManagers = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof Contribution) {
                Contribution c = (Contribution)contrib;
                c.setExtension(extension);
                ContributionManager mgr = contributionManagers.get(c.getExtensionPoint());
                if (mgr != null) {
                    mgr.registerContribution(c);
                } else {
                    log.warn("Unable to register contribution: "
                            + c.getContributionId() + " for extension point "
                            + c.getExtensionPoint()
                            + ". No manager registered.");
                }
            } else {
                registerContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof Contribution) {
                Contribution c = (Contribution)contrib;
                c.setExtension(extension);
                ContributionManager mgr = contributionManagers.get(c.getExtensionPoint());
                if (mgr != null) {
                    mgr.unregisterContribution(c);
                } else {
                    log.warn("Unable to unregister contribution: "
                            + c.getContributionId() + " for extension point "
                            + c.getExtensionPoint()
                            + ". No manager registered.");
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
