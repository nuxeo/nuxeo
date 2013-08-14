/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
            blobHolder = new MailMessageBlobHolder(doc,
                    MailCoreConstants.HTML_TEXT_PROPERTY_NAME, "body.html");
        } else {
            blobHolder = null;
        }
        return blobHolder;
    }

}
