/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.mail.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderFactory;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;

/**
 * Return appropriate BlobHolder for MailMessage documents.
 *
 * @author ldoguin
 * @since 5.7.3
 */
public class MailMessageBlobHolderfactory implements BlobHolderFactory {

    @Override
    public BlobHolder getBlobHolder(DocumentModel doc) {
        String docType = doc.getType();
        BlobHolder blobHolder;

        if (docType.equals(MailCoreConstants.MAIL_MESSAGE_TYPE)) {
            blobHolder = new MailMessageBlobHolder(doc, MailCoreConstants.HTML_TEXT_PROPERTY_NAME, "body.html");
        } else {
            blobHolder = null;
        }
        return blobHolder;
    }

}
