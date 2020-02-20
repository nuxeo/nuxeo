/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.ec.notification.automation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;

import java.util.List;

/**
 * Operation to unsubscribe a user to the notifications of one or more of documents.
 *
 * @since 8.10
 */
@Operation(id = UnsubscribeOperation.ID, category = Constants.CAT_DOCUMENT, label = "Unsubscribe document",
    description = "Unsubscribe one or more documents. No value is returned.")
public class UnsubscribeOperation {

    public static final String ID = "Document.Unsubscribe";

    @Context
    protected CoreSession coreSession;

    @Context
    protected NotificationManager notificationManager;

    @Param(name = "notifications", required = false)
    protected StringList notifications;

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        docs.forEach(this::run);
        return docs;
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        NuxeoPrincipal principal = coreSession.getPrincipal();
        String username = NotificationConstants.USER_PREFIX + principal.getName();
        if (notifications == null || notifications.isEmpty()) {
            // unsubscribe all available notifications
            List<String> userSubscriptions = notificationManager.getSubscriptionsForUserOnDocument(username, doc);
            notificationManager.removeSubscriptions(username, userSubscriptions, doc);
        } else {
            // unsubscribe the specified notifications
            for (String notification : notifications) {
                notificationManager.removeSubscription(username, notification, doc);
            }
        }
        return doc;
    }

}
