/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.migration.service;

import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Compat service displaying migration information for old extension points.
 *
 * @since 7.4
 */
public class ThemeMigrationService extends DefaultComponent {

    protected static final String XP = "org.nuxeo.theme.services.ThemeService";

    protected static final String WR_XP = "org.nuxeo.ecm.platform.WebResources";

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("resources".equals(extensionPoint)) {
            if (contribution instanceof ResourceDescriptor) {
                ResourceDescriptor r = (ResourceDescriptor) contribution;
                String message = String.format("Resource '%s' on component %s should now be contributed to extension "
                        + "point '%s': a compatibility registration was performed but it may not be " + "accurate.",
                        r.getName(), contributor.getName(), WR_XP);
                DeprecationLogger.log(message, "7.4");
                Framework.getRuntime().getWarnings().add(message);
                // ensure path is absolute, consider that resource is in the war, and if not, user will have to declare it
                // directly to the WRM endpoint
                String path = r.getPath();
                if (path != null && !path.startsWith("/")) {
                    r.setUri("/" + path);
                }
                WebResourceManager wrm = Framework.getService(WebResourceManager.class);
                wrm.registerResource(r);
            } else {
                String message = String.format("Warning: unknown contribution to target extension point '%s' of '%s'. "
                        + "Check your extension in component %s", extensionPoint, XP, contributor.getName());
                DeprecationLogger.log(message, "7.4");
                Framework.getRuntime().getWarnings().add(message);
            }
        } else {
            String message = String.format("Warning: target extension point '%s' of '%s'"
                    + " is unknown. Check your extension in component %s", extensionPoint, XP, contributor.getName());
            DeprecationLogger.log(message, "7.4");
            Framework.getRuntime().getWarnings().add(message);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("resources".equals(extensionPoint) && contribution instanceof ResourceDescriptor) {
            ResourceDescriptor r = (ResourceDescriptor) contribution;
            WebResourceManager wrm = Framework.getService(WebResourceManager.class);
            wrm.unregisterResource(r);
        } else {
            // NOOP
        }
    }

}