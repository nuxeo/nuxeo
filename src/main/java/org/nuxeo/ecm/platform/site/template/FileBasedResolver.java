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

package org.nuxeo.ecm.platform.site.template;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileBasedResolver implements SiteObjectResolver {

    protected File root;

    public FileBasedResolver(SiteManager mgr) {
        this.root = new File(mgr.getRootDirectory(), "templates");
        if (mgr.getDefaultSiteObject() == null) {
            mgr.setDefaultSiteObject(createSiteObject("default", null));
        }
    }

    public SitePageTemplate resolve(DocumentModel doc, Map<String, SitePageTemplate> objects) {
        String type = doc.getType();
        SitePageTemplate obj = objects.get(type);
        if (obj == null) {
            obj = createSiteObject(type, "default");
            if (obj != null) {
                objects.put(type, obj);
            }
        }
        return obj;
    }


    protected SitePageTemplate createSiteObject(String type, String zuper) {
        SitePageTemplate obj = null;
        File typeFolder = new File(root, type);
        if (typeFolder.isDirectory()) {
            obj = new SitePageTemplate(type, zuper);
            for (String name : typeFolder.list()) {
                if (name.endsWith(".ftl")) {
                    try {
                        SiteObjectView view = new SiteObjectView(
                                name.substring(0, name.length()-4),
                                new File(typeFolder, name).toURI().toURL());
                        obj.addView(view);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return obj;
    }

}
