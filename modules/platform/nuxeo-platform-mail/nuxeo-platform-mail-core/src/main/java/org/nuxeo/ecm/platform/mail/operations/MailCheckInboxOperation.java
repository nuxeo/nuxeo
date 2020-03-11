/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      André Justo
 */
package org.nuxeo.ecm.platform.mail.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mail.utils.MailCoreHelper;

import javax.mail.MessagingException;

/**
 * Checks for unread emails in the inbox of an Email Folder.
 *
 * @since 10.1
 */
@Operation(id = MailCheckInboxOperation.ID, category = Constants.CAT_SERVICES, label = "Check Mail Inbox", description = "Checks for unread emails in the inbox of an Email Folder passed as input.")
public class MailCheckInboxOperation {

    public static final String ID = "Mail.CheckInbox";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run(DocumentModel document) throws MessagingException {
        MailCoreHelper.checkMail(document, session);
    }
}
