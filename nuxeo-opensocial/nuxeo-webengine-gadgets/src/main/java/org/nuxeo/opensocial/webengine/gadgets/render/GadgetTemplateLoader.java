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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.opensocial.webengine.gadgets.render;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;

import freemarker.cache.StringTemplateLoader;

/**
 * Specific template loader that fallsback to lookup in skin/resources/ftl for common template files
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class GadgetTemplateLoader extends StringTemplateLoader {

    protected static Log log = LogFactory.getLog(GadgetTemplateLoader.class);

    @Override
    public Object findTemplateSource(String name) {
        Object template = super.findTemplateSource(name);

        if (template==null) {
            // fallback to lookup in common resources
            InputStream stream = GadgetTemplateLoader.class.getClassLoader().getResourceAsStream("skin/resources/ftl/" + name);
            if (stream!=null) {
                try {
                    String templateSource = FileUtils.read(stream);
                    super.putTemplate(name, templateSource);
                    template = super.findTemplateSource(name);
                } catch (IOException e) {
                    log.error("Unable to find template " + name, e);
                }
            }
        }
        return template;
    }
}
