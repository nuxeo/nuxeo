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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

public class DynamicTemplateSiteObjectHandler extends DefaultSiteObjectHandler {

    protected String templateKey;

    public DynamicTemplateSiteObjectHandler() {
    }

    public DynamicTemplateSiteObjectHandler(DocumentModel doc) {
        super(doc);
    }

    protected String getDynamicTemplateKey(SiteRequest request, String schemaName,
            String fieldName) {
        return getDynamicTemplateKey(request, schemaName, fieldName, false);
    }

    protected String getDynamicTemplateKey(SiteRequest request, String schemaName, String fieldName,
            boolean preProcess) {
        if (templateKey == null) {
            String content = (String) sourceDocument.getProperty(schemaName, fieldName);
            if (preProcess) {
                content = preprocessTemplateContent(request, content);
            }
            templateKey = getTemplateManager().registerDynamicTemplate(this, content);
        }
        return templateKey;
    }

    protected String preprocessTemplateContent(SiteRequest request, String templateContent) {
        return templateContent;
    }

    protected String addEditLink(SiteRequest request, String templateContent) {
        String editLinkStart = "<A href=\"";
        String editLinkEnd = "\">Edit</A><BR/>";

        String url = request.getRequestURI() + "?" + SiteRequest.MODE_KEY + "=" + SiteRequest.EDIT_MODE;


        return editLinkStart + url + editLinkEnd + templateContent;
    }


}
