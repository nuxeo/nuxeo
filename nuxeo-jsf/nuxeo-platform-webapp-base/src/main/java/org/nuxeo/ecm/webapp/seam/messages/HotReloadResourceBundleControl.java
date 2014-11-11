/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.seam.messages;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * Resource bundle controller for Seam.
 * <p>
 * Handles hot reload of resources when in dev mode, relying on the
 * {@link ReloadService#flush()} method to be called when needing to flush
 * messages.
 *
 * @since 5.6
 */
@Name("nxHotReloadResourceBundleControl")
@BypassInterceptors
@Scope(ScopeType.SESSION)
@AutoCreate
public class HotReloadResourceBundleControl extends ResourceBundle.Control {

    protected long timeToLive = ResourceBundle.Control.TTL_NO_EXPIRATION_CONTROL;

    public static HotReloadResourceBundleControl instance() {
        return (HotReloadResourceBundleControl) Component.getInstance(
                HotReloadResourceBundleControl.class, true);
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale,
            String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        // force reload if debug mode is set
        return super.newBundle(baseName, locale, format, loader,
                Framework.isDevModeSet());
    }

}
