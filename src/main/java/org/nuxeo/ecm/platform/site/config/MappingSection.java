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

package org.nuxeo.ecm.platform.site.config;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.site.SiteRoot;
import org.nuxeo.ecm.platform.site.mapping.MappingDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MappingSection implements Configurator {

    private final static Log log = LogFactory.getLog(MappingSection.class);
    public final static MappingSection INSTANCE = new MappingSection();

    public void configure(SiteRoot root, Map<String, String> properties) {
        MappingDescriptor md = new MappingDescriptor();
        String val = properties.get("pattern");
        if (val == null) {
            log.warn("Found mapping section without pattern property. Ignoring ...");
            return;
        }
        md.setPattern(val);
        val = properties.get("script");
        md.setScript(val);
        val = properties.get("traversal");
        md.setTraversal(val);
        root.getPathMapper().addMapping(md);
    }

}
