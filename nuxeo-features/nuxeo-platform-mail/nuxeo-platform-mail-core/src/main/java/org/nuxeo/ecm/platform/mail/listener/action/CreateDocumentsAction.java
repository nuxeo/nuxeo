/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.ATTACHMENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CC_RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CC_RECIPIENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.HTML_TEXT_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_MESSAGE_TYPE;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MESSAGE_ID_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MESSAGE_ID_PROPERTY_NAME;
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Creates a MailMessage document for every new email found in the INBOX. The properties values are retrieved from the
 * pipe execution context.
 *
 * @author Catalin Baican
 */
public class CreateDocumentsAction extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(CreateDocumentsAction.class);

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(ExecutionContext context) {
        CoreSession session = getCoreSession(context);
        if (session == null) {
            log.error("Could not open CoreSession");
            return false;
        }
        PathSegmentService pss = Framework.getService(PathSegmentService.class);

        ExecutionContext initialContext = context.getInitialContext();

        String subject = (String) context.get(SUBJECT_KEY);
        String sender = (String) context.get(SENDER_KEY);
        Date sendingDate = (Date) context.get(SENDING_DATE_KEY);
        ArrayList<String> recipients = (ArrayList<String>) context.get(RECIPIENTS_KEY);
        ArrayList<String> ccRecipients = (ArrayList<String>) context.get(CC_RECIPIENTS_KEY);
        List<FileBlob> attachments = (List<FileBlob>) context.get(ATTACHMENTS_KEY);
        String text = (String) context.get(TEXT_KEY);
        String messageId = (String) context.get(MESSAGE_ID_KEY);

        String parentPath = (String) initialContext.get(PARENT_PATH_KEY);

        DocumentModel documentModel = session.createDocumentModel(MAIL_MESSAGE_TYPE);
        documentModel.setPropertyValue("dc:title", subject + System.currentTimeMillis());
        documentModel.setPathInfo(parentPath, pss.generatePathSegment(documentModel));
        documentModel.setPropertyValue("dc:title", subject);
        documentModel.setPropertyValue(MESSAGE_ID_PROPERTY_NAME, messageId);
        documentModel.setPropertyValue(SENDER_PROPERTY_NAME, sender);
        documentModel.setPropertyValue(SENDING_DATE_PROPERTY_NAME, sendingDate);
        documentModel.setPropertyValue(RECIPIENTS_PROPERTY_NAME, recipients);
        documentModel.setPropertyValue(CC_RECIPIENTS_PROPERTY_NAME, ccRecipients);
        if (attachments != null && !attachments.isEmpty()) {
            ArrayList<Map<String, Serializable>> files = new ArrayList<>();
            for (FileBlob currentFileBlob : attachments) {
                if (currentFileBlob != null) {
                    Map<String, Serializable> file = new HashMap<>();
                    file.put("file", currentFileBlob);
                    files.add(file);
                }
            }
            documentModel.setPropertyValue("files:files", files);
        }
        documentModel.setPropertyValue(CC_RECIPIENTS_PROPERTY_NAME, ccRecipients);

        documentModel.setPropertyValue(HTML_TEXT_PROPERTY_NAME, text);
        if (text != null && !text.isEmpty()) {
            Blob sb = Blobs.createBlob(text);
            BlobHolder simpleBlobHolder = new SimpleBlobHolder(sb);
            ConversionService conversionService = Framework.getService(ConversionService.class);
            Map<String, Serializable> parameters = new HashMap<>();
            parameters.put("tagFilter", "body");
            BlobHolder simpleTextBH = conversionService.convert("html2text", simpleBlobHolder, parameters);
            String simpleText;
            try {
                simpleText = simpleTextBH.getBlob().getString();
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            documentModel.setPropertyValue(TEXT_PROPERTY_NAME, simpleText);
        }

        UnrestrictedCreateDocument unrestrictedCreateDocument = new UnrestrictedCreateDocument(documentModel, session);
        unrestrictedCreateDocument.runUnrestricted();

        return true;
    }

    // Helper inner class to do the unrestricted creation of the documents
    protected class UnrestrictedCreateDocument extends UnrestrictedSessionRunner {

        private DocumentModel document;

        protected UnrestrictedCreateDocument(DocumentModel document, CoreSession session) {
            super(session);
            this.document = document;
        }

        @Override
        public void run() {
            document = session.createDocument(document);
            document = session.saveDocument(document);
            session.save();
        }
    }

}
