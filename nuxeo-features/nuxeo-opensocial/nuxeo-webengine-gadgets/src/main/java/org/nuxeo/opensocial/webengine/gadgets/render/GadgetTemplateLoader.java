/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     eugen
 */
package org.nuxeo.opensocial.webengine.gadgets.render;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.gadgets.service.InternalGadgetDescriptor;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

import freemarker.cache.URLTemplateLoader;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 * 
 */
public class GadgetTemplateLoader extends URLTemplateLoader {

    private static final Log log = LogFactory.getLog(GadgetTemplateLoader.class);

    protected URL getURL(String name) {
        if (name.startsWith("gadget://")) {
            GadgetService gs = Framework.getLocalService(GadgetService.class);
            if (gs == null)
                return null;
            GadgetDeclaration gadget = gs.getGadget(name.substring("gadget://".length()));

            try {
                return gadget.getResource(((InternalGadgetDescriptor) gadget).getEntryPoint());
            } catch (IOException e) {
                log.debug("failed to gadget entry point", e);
            }
        } else {
            // fallback to lookup in common resources
            return GadgetTemplateLoader.class.getClassLoader().getResource(
                    "skin/resources/ftl/" + name);
        }
        return null;
    }
}
