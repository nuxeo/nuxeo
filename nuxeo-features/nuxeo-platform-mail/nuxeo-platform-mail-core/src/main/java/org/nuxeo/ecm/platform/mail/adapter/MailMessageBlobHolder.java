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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;

/**
 * BlobHolder for MailMessage documents. The blob returned is a StringBlob with
 * the mail body message as content.
 * 
 * @author ldoguin
 * @since 5.7.3
 */
public class MailMessageBlobHolder extends DocumentBlobHolder {

    protected Pattern isHtmlPattern = Pattern.compile(
            "(.*)<(html|head|body)>(.*)", Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);

    public MailMessageBlobHolder(DocumentModel doc, String xPath,
            String xPathFilename) {
        super(doc, xPath, xPathFilename);
    }

    @Override
    public Blob getBlob() throws ClientException {
        String htmlTextProperty = (String) doc.getPropertyValue(xPath);
        Blob blob = null;
        if (htmlTextProperty != null && xPathFilename != null
                && htmlTextProperty.length() != 0) {
            blob = new StringBlob(htmlTextProperty);
            Matcher m = isHtmlPattern.matcher(htmlTextProperty);
            if (m.matches()) {
                blob.setMimeType("text/html");
            } else {
                blob.setMimeType("text/plain");
            }
        } else {
            String txt = (String) doc.getPropertyValue(MailCoreConstants.TEXT_PROPERTY_NAME);
            if (txt == null) {
                txt = "";
            }
            blob = new StringBlob(txt, "text/plain");
        }
        if (blob != null) {
            blob.setFilename(xPathFilename);
            // set dummy digest to avoid comparison error
            blob.setDigest("notInBinaryStore");
        }
        return blob;
    }
}
