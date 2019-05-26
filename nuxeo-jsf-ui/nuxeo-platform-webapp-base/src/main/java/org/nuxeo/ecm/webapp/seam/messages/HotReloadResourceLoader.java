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

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Locale;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;

/**
 * Overriden to add a control over reload of bundle for hot reload
 *
 * @since 5.6
 */
@Scope(ScopeType.STATELESS)
@BypassInterceptors
@Name("org.jboss.seam.core.resourceLoader")
// XXX: since debug mode cannot be set by using nuxeo debug/dev mode, make
// sure this component is deployed even in production => debug = false
@Install(precedence = APPLICATION, debug = false)
public class HotReloadResourceLoader extends ResourceLoader {

    private static final LogProvider log = Logging.getLogProvider(HotReloadResourceLoader.class);

    @Override
    public ResourceBundle loadBundle(String bundleName) {
        try {
            ResourceBundle.Control control = HotReloadResourceBundleControl.instance();
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.instance(),
                    Thread.currentThread().getContextClassLoader(), control);
            log.debug("loaded resource bundle: " + bundleName);
            return bundle;
        } catch (MissingResourceException mre) {
            log.debug("resource bundle missing: " + bundleName);
            return null;
        }
    }
}
