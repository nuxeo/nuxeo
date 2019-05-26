/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.seam.messages;

import java.io.IOException;
import java.io.Serializable;
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
 * Handles hot reload of resources when in dev mode, relying on the {@link ReloadService#flush()} method to be called
 * when needing to flush messages.
 *
 * @since 5.6
 */
@Name("nxHotReloadResourceBundleControl")
@BypassInterceptors
@Scope(ScopeType.SESSION)
@AutoCreate
public class HotReloadResourceBundleControl extends ResourceBundle.Control implements Serializable {

    private static final long serialVersionUID = 1L;

    protected long timeToLive = ResourceBundle.Control.TTL_NO_EXPIRATION_CONTROL;

    public static HotReloadResourceBundleControl instance() {
        return (HotReloadResourceBundleControl) Component.getInstance(HotReloadResourceBundleControl.class, true);
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        // force reload if debug mode is set
        return super.newBundle(baseName, locale, format, loader, Framework.isDevModeSet());
    }

}
