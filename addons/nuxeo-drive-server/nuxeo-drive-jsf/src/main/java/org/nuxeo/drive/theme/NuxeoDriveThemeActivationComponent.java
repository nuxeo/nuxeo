/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.theme;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * Component used to deploy the Nuxeo Drive theme contribution only when not
 * using hot-reload to avoid the Nuxeo IDE crash at login. Uses the
 * {@link #activate(ComponentContext)} method.
 */
public class NuxeoDriveThemeActivationComponent extends DefaultComponent {

    private static final String THEME_CONTRIB = "OSGI-INF/nuxeodrive-theme.xml";

    protected RegistrationInfo themeInfo;

    @Override
    public void activate(ComponentContext context) throws Exception {
        if (isSDKContainer()) {
            return; // skip them contribution deployment, breaks JSF hot reload
        }
        themeInfo = context.getRuntimeContext().deploy(THEME_CONTRIB);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (themeInfo == null) {
            return;
        }
        try {
            themeInfo.getContext().undeploy(THEME_CONTRIB);
        } finally {
            themeInfo = null;
        }
    }

    boolean isSDKContainer() {
        return "org.nuxeo.runtime.tomcat.dev".equals(this.getClass().getClassLoader().getClass().getPackage().getName());

    }

}
