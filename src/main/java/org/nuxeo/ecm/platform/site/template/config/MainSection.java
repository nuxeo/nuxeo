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

package org.nuxeo.ecm.platform.site.template.config;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.site.resolver.SiteResourceResolver;
import org.nuxeo.ecm.platform.site.template.SiteRoot;
import org.nuxeo.runtime.Version;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MainSection implements Configurator {

    private final static Log log = LogFactory.getLog(MainSection.class);
    public final static MainSection INSTANCE = new MainSection();

    @SuppressWarnings("unchecked")
    public void configure(SiteRoot root, Map<String, String> properties) {
        String val = properties.get("version");
        if (val != null) {
            root.setVersion(Version.parseString(val));
        }
        val = properties.get("debug");
        if (val != null) {
            root.setDebugEnabled(Boolean.getBoolean(val));
        }
        val = properties.get("resolver");
        if (val != null) {
            try {
                Class<SiteResourceResolver> resolverClass = (Class<SiteResourceResolver>)Class.forName(val);
                root.setResolver(resolverClass.newInstance());
            } catch (Exception e) {
                log.error("Failed to instantiate resolver: "+val, e);
            }
        }
    }

}
