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
 */
package org.nuxeo.ecm.webengine.debug;

import org.nuxeo.ecm.webengine.model.impl.ModuleImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultModuleTracker extends ModuleTracker {

    protected ModuleClassesEntry classes;

    public DefaultModuleTracker(ModuleImpl module) {
        super(module);
        classes = new ModuleClassesEntry(module.getRoot());
    }

    @Override
    protected void doRun() throws Exception {
        super.doRun();
        if (classes.check()) { // classes changed - reload class loader
            // reload class loaders
            module.getEngine().getWebLoader().flushCache();
            // remove registered types (which are using older version of classes)
            flushTypeCache(module);
            // re-register main entry point?
            //module.getEngine().registerRootBinding();
            // to speed up things we also invalidate skin cache and then return
            flushSkinCache(module);
            return;
        }
    }

}
