/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.webengine.sites.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;

public class ContextTransmitterHelper {

    private ContextTransmitterHelper() {
    }

    public static void feedContext(DocumentModel doc) {
        WebContext ctx = WebEngine.getActiveContext();
        String basePath = ctx.getModulePath();

        Resource target = ctx.getTargetObject();
        Resource parentResource = target.getPrevious();
        while (parentResource != null && !parentResource.isInstanceOf(SiteConstants.WEBSITE)) {
            parentResource = parentResource.getPrevious();
        }
        String siteName = "";
        if (parentResource != null) {
            siteName = parentResource.getName();
        }
        String targetObjectPath = target.getPath();
        doc.getContextData().putScopedValue("basePath", basePath);
        doc.getContextData().putScopedValue("siteName", siteName);
        doc.getContextData().putScopedValue("targetObjectPath", targetObjectPath);
    }

}
