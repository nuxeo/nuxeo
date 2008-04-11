/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.site.adapters;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

/**
 * Default impl SiteObject DocumentModel adapter
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */

public class DefaultSiteObjectHandler extends AbstractSiteObjectHandler {

    public DefaultSiteObjectHandler() {

    }

    public DefaultSiteObjectHandler(DocumentModel doc) {
        sourceDocument = doc;
    }

    public void doGet(SiteRequest request, HttpServletResponse response) {
        // TODO Auto-generated method stub
    }

    public String getTemplateName(SiteRequest request) {
        return getTemplateManager().getTemplateNameForDoc(sourceDocument);
    }

}
