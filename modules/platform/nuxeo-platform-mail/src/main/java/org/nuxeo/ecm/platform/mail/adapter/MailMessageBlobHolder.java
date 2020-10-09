/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.mail.adapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;

/**
 * BlobHolder for MailMessage documents. The blob returned is a StringBlob with the mail body message as content.
 *
 * @author ldoguin
 * @since 5.7.3
 */
public class MailMessageBlobHolder extends DocumentBlobHolder {

    protected Pattern isHtmlPattern = Pattern.compile("(.*)<(html|head|body)>(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final String filename;

    public MailMessageBlobHolder(DocumentModel doc, String xPath, String filename) {
        super(doc, xPath);
        this.filename = filename;
    }

    @Override
    public Blob getBlob() {
        String htmlTextProperty = (String) doc.getPropertyValue(xPath);
        Blob blob;
        if (htmlTextProperty != null && filename != null && htmlTextProperty.length() != 0) {
            blob = Blobs.createBlob(htmlTextProperty);
            Matcher m = isHtmlPattern.matcher(htmlTextProperty);
            if (m.matches()) {
                blob.setMimeType("text/html");
            } // else default is text/plain
        } else {
            String txt = (String) doc.getPropertyValue(MailCoreConstants.TEXT_PROPERTY_NAME);
            if (txt == null) {
                txt = "";
            }
            blob = Blobs.createBlob(txt);
        }
        blob.setFilename(filename);
        // set dummy digest to avoid comparison error
        blob.setDigest("notInBinaryStore");
        return blob;
    }
}
