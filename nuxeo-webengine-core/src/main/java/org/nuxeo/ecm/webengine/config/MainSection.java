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

package org.nuxeo.ecm.webengine.config;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.DocumentResolver;
import org.nuxeo.ecm.webengine.SiteRoot;
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
            root.setDebugEnabled(Boolean.parseBoolean(val));
        }
        val = properties.get("defaultPage");
        if (val != null) {
            root.setDefaultPage(val);
        }
        val = properties.get("errorPage");
        if (val != null) {
            root.setErrorPage(val);
        }
        val = properties.get("resolver");
        if (val != null) {
            try {
                Class<DocumentResolver> resolverClass = (Class<DocumentResolver>)Class.forName(val);
                root.setResolver(resolverClass.newInstance());
            } catch (Exception e) {
                log.error("Failed to instantiate resolver: "+val, e);
            }
        }
    }

}
