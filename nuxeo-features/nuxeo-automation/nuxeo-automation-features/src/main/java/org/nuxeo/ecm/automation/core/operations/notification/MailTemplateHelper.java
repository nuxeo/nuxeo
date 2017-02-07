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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MailTemplateHelper {

    protected static final Log log = LogFactory.getLog(MailTemplateHelper.class);

    private MailTemplateHelper() {
    }

    public static String getDocumentUrl(DocumentModel doc, String viewId) {
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        DocumentViewCodecManager codecService = Framework.getService(DocumentViewCodecManager.class);
        DocumentViewCodec codec = codecService.getCodec(NotificationEventListener.NOTIFICATION_DOCUMENT_ID_CODEC_NAME);
        boolean isNotificationCodec = codec != null;

        String result = "";
        if (isNotificationCodec) {
            DocumentView view;
            if (viewId == null) {
                view = new DocumentViewImpl(doc);
            } else {
                view = new DocumentViewImpl(new DocumentLocationImpl(doc), viewId);
            }
            result = codecService.getUrlFromDocumentView(NotificationEventListener.NOTIFICATION_DOCUMENT_ID_CODEC_NAME,
                    view, true, notificationService.getServerUrlPrefix());
        } else {
            log.warn("No codec was found to notify document url. It is like that no UI is installed.");
        }
        return result;
    }

    public static URL getTemplate(String name) {
        return NotificationService.getTemplateURL(name);
    }

}
