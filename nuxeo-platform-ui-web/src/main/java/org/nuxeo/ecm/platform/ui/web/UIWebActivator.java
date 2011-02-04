/*
 * (C) Copyright 2007-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 *
 */

package org.nuxeo.ecm.platform.ui.web;

import org.nuxeo.ecm.platform.ui.web.util.beans.PropertiesEditorsInstaller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class UIWebActivator implements BundleActivator {

    PropertiesEditorsInstaller editorsInstaller = new PropertiesEditorsInstaller();

    @Override
    public void start(BundleContext context) throws Exception {
        editorsInstaller.installEditors();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        editorsInstaller.uninstallEditors();
    }

}
