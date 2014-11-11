package org.nuxeo.runtime.jtajca;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

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

/**
 * If this bundle is present in the running platform it should automatically
 * install the NuxeoContainer.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JtaActivator extends DefaultComponent {

    public static final String AUTO_ACTIVATION = "NuxeoContainer.autoactivation";

    @Override
    public void activate(ComponentContext context) throws Exception {
        final String property = Framework.getProperty(AUTO_ACTIVATION);
        if (property == null || "false".equalsIgnoreCase(property)) {
            return;
        }
        NuxeoContainer.install();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (NuxeoContainer.isInstalled()) {
            NuxeoContainer.uninstall();
        }
    }

}
