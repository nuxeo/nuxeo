/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platfrom.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.ATTACHMENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CC_RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CC_RECIPIENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_MESSAGE_TYPE;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.RECIPIENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDER_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDER_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDING_DATE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDING_DATE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SUBJECT_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.TEXT_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.TEXT_PROPERTY_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
 * Creates a MailMessage document for every new email found in the INBOX. The
 * properties values are retrieved from the pipe execution context.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class CreateDocumentsAction extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(CreateDocumentsAction.class);

    @SuppressWarnings("unchecked")
    public boolean execute(ExecutionContext context) throws Exception {
        CoreSession session = getCoreSession(context);
        if (session == null) {
            log.error("Could not open CoreSession");
            return false;
        }

        ExecutionContext initialContext = context.getInitialContext();

        String subject = (String) context.get(SUBJECT_KEY);
        String sender = (String) context.get(SENDER_KEY);
        Date sendingDate = (Date) context.get(SENDING_DATE_KEY);
        ArrayList<String> recipients = (ArrayList<String>) context.get(RECIPIENTS_KEY);
        ArrayList<String> ccRecipients = (ArrayList<String>) context.get(CC_RECIPIENTS_KEY);
        List<FileBlob> attachments = (List<FileBlob>) context.get(ATTACHMENTS_KEY);
        String text = (String) context.get(TEXT_KEY);

        String parentPath = (String) initialContext.get(PARENT_PATH_KEY);

        DocumentModel documentModel = session.createDocumentModel(parentPath,
                IdUtils.generateId(subject + System.currentTimeMillis()),
                MAIL_MESSAGE_TYPE);
        documentModel.setPropertyValue("dc:title", subject);
        documentModel.setPropertyValue(SENDER_PROPERTY_NAME, sender);
        documentModel.setPropertyValue(SENDING_DATE_PROPERTY_NAME, sendingDate);
        documentModel.setPropertyValue(RECIPIENTS_PROPERTY_NAME, recipients);
        documentModel.setPropertyValue(CC_RECIPIENTS_PROPERTY_NAME,
                ccRecipients);
        if (attachments != null && !attachments.isEmpty()) {
            ArrayList<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
            for (FileBlob currentFileBlob : attachments) {
                if (currentFileBlob != null) {
                    Map<String, Serializable> file = new HashMap<String, Serializable>();
                    file.put("file", currentFileBlob);
                    file.put("filename", currentFileBlob.getFilename());
                    files.add(file);
                }
            }
            documentModel.setPropertyValue("files:files", files);
        }
        documentModel.setPropertyValue(CC_RECIPIENTS_PROPERTY_NAME,
                ccRecipients);
        documentModel.setPropertyValue(TEXT_PROPERTY_NAME, text);

        UnrestrictedCreateDocument unrestrictedCreateDocument = new UnrestrictedCreateDocument(
                documentModel, session);
        unrestrictedCreateDocument.runUnrestricted();

        return true;
    }

    public void reset(ExecutionContext context) throws Exception {
        // do nothing
    }

    // Helper inner class to do the unrestricted creation of the documents
    protected class UnrestrictedCreateDocument extends
            UnrestrictedSessionRunner {

        private DocumentModel document;

        protected UnrestrictedCreateDocument(DocumentModel document,
                CoreSession session) {
            super(session);
            this.document = document;
        }

        @Override
        public void run() throws ClientException {
            document = session.createDocument(document);
            document = session.saveDocument(document);
            session.save();
        }
    }

}
