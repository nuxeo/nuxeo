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

package org.nuxeo.ecm.webengine.config;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ManagedComponent extends DefaultComponent {

    protected ContributionManager contributionManager;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contributionManager = new ContributionManager(this);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        contributionManager = null;
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
                contributionManager.registerContribution(c);
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
                contributionManager.unregisterContribution(c);
            } else {
                unregisterContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
            }
        }
    }

    /**
     *
     * @param contrib
     * @throws Exception
     */
    protected void registerContribution(Contribution contrib) throws Exception {
        // extend this
    }

    /**
     *
     * @param contrib
     * @throws Exception
     */
    protected void unregisterContribution(Contribution contrib) throws Exception {
        // extend this
    }


}
