/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import java.net.URL;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MailTemplateHelper {

    public static String getDocumentUrl(DocumentModel doc, String viewId) throws Exception {
        if (viewId == null) {
            viewId = "view_documents";
        }
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId(viewId);
        return Framework.getService(DocumentViewCodecManager.class).getUrlFromDocumentView(
                        docView,
                        true,
                        NotificationServiceHelper.getNotificationService().getServerUrlPrefix());

    }

    public static URL getTemplate(String name) {
        return NotificationService.getTemplateURL(name);
    }

}
