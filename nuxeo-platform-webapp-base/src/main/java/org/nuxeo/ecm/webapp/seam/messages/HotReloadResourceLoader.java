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
//XXX: since debug mode cannot be set by using nuxeo debug/dev mode, make
//sure this component is deployed even in production => debug = false
@Install(precedence = APPLICATION, debug = false)
public class HotReloadResourceLoader extends ResourceLoader {

    private static final LogProvider log = Logging.getLogProvider(HotReloadResourceLoader.class);

    @Override
    public ResourceBundle loadBundle(String bundleName) {
        try {
            ResourceBundle.Control control = HotReloadResourceBundleControl.instance();
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName,
                    Locale.instance(),
                    Thread.currentThread().getContextClassLoader(), control);
            log.debug("loaded resource bundle: " + bundleName);
            return bundle;
        } catch (MissingResourceException mre) {
            log.debug("resource bundle missing: " + bundleName);
            return null;
        }
    }
}