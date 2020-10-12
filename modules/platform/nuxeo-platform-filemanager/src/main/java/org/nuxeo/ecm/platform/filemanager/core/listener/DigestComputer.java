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
package org.nuxeo.ecm.platform.filemanager.core.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

public class DigestComputer implements EventListener {

    private static final Log log = LogFactory.getLog(DigestComputer.class);

    private void addDigestToDocument(DocumentModel doc) {
        FileManager fm = Framework.getService(FileManager.class);
        List<String> xpathFields = fm.getFields();
        String digestAlgo = fm.getDigestAlgorithm();
        for (String xpathField : xpathFields) {
            Property blobProp = null;
            try {
                blobProp = doc.getProperty(xpathField);
            } catch (PropertyException e) {
                log.debug("Property " + xpathField + " not found on doc, skipping");
            }
            if (blobProp != null && !blobProp.isPhantom() && blobProp.isDirty()) {
                try {
                    Blob blob = (Blob) blobProp.getValue();
                    if (blob != null) {
                        String digest = computeDigest(blob, digestAlgo);
                        if (!digest.equals(blob.getDigest())) {
                            blob.setDigest(digest);
                        }
                    }
                } catch (PropertyException | IOException e) {
                    log.error("Error while trying to save blob digest", e);
                }
            }
        }
    }

    private String computeDigest(Blob blob, String digestAlgo) throws IOException {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance(digestAlgo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] b = DigestUtils.updateDigest(md, blob.getStream()).digest();
        return Base64.encodeBase64String(b);
    }

    @Override
    public void handleEvent(Event event) {
        FileManager fm = Framework.getService(FileManager.class);
        if (!fm.isDigestComputingEnabled()) {
            return;
        }

        EventContext ctx = event.getContext();
        String evt = event.getName();
        if (ABOUT_TO_CREATE.equals(evt) || BEFORE_DOC_UPDATE.equals(evt)) {

            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                DocumentModel doc = docCtx.getSourceDocument();
                if (doc == null || doc.isProxy()) {
                    return;
                }
                addDigestToDocument(doc);
            }
        }
    }

}
