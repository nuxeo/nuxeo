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

package org.nuxeo.ecm.platform.site.template;

import java.io.File;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteManagerComponent extends DefaultComponent {

    public final static ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.site.template.SiteManagerComponent");

    private SiteManager mgr;
    private FileChangeNotifier notifier;

    @Override
    public void activate(ComponentContext context) throws Exception {
        notifier = new FileChangeNotifier();
        notifier.start();
        File root = new File(Framework.getRuntime().getHome(), "web");
        mgr = new SiteManagerImpl(root);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // TODO Auto-generated method stub
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // TODO Auto-generated method stub
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // TODO Auto-generated method stub
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == SiteManager.class) {
            return adapter.cast(mgr);
        } else if (adapter == FileChangeNotifier.class) {
            return adapter.cast(notifier);
        }
        return null;
    }

}
