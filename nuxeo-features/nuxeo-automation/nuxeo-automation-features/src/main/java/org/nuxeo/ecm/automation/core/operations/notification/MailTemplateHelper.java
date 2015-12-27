/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class MailTemplateHelper {

    private MailTemplateHelper() {
    }

    public static String getDocumentUrl(DocumentModel doc, String viewId) {
        if (viewId == null) {
            viewId = "view_documents";
        }
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId(viewId);
        DocumentViewCodecManager codecMgr = Framework.getService(DocumentViewCodecManager.class);
        NotificationService notifMgr = NotificationServiceHelper.getNotificationService();
        if (codecMgr == null) {
            throw new RuntimeException("Service 'DocumentViewCodecManager' not available");
        }
        if (notifMgr == null) {
            throw new RuntimeException("Service 'NotificationService' not available");
        }
        return codecMgr.getUrlFromDocumentView(docView, true, notifMgr.getServerUrlPrefix());
    }

    public static URL getTemplate(String name) {
        return NotificationService.getTemplateURL(name);
    }

}
