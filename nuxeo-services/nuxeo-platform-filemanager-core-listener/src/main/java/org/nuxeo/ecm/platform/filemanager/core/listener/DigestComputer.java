/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

public class DigestComputer implements EventListener {

    private boolean initDone = false;

    private List<String> xpathFields;

    private String digestAlgo = "sha-256";

    private Boolean activateDigestComputation = false;

    private static final Log log = LogFactory.getLog(DigestComputer.class);

    private boolean initIfNeeded() {
        if (!initDone) {
            try {
                FileManager fm = Framework.getService(FileManager.class);
                xpathFields = fm.getFields();
                digestAlgo = fm.getDigestAlgorithm();
                activateDigestComputation = fm.isDigestComputingEnabled();
                initDone = true;
            } catch (Exception e) {
                log.error("Unable to initialize Digest Computer Core Listener",
                        e);
            }
        }

        return initDone;
    }

    private void addDigestToDocument(DocumentModel doc) {
        for (String xpathField : xpathFields) {
            Property blobProp = null;
            try {
                blobProp = doc.getProperty(xpathField);
            } catch (PropertyException e) {
                log.debug("Property " + xpathField
                        + " not found on doc, skipping");
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            if (blobProp != null && !blobProp.isPhantom() && blobProp.isDirty()) {
                try {
                    Blob blob = (Blob) blobProp.getValue();
                    if (blob != null) {
                        String digest = computeDigest(blob);
                        if (!digest.equals(blob.getDigest())) {
                            blob.setDigest(digest);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while trying to save blob digest", e);
                }
            }
        }
    }

    private String computeDigest(Blob blob) throws NoSuchAlgorithmException,
            IOException {

        MessageDigest md = MessageDigest.getInstance(digestAlgo);

        // make sure the blob can be read several times without exhausting its
        // binary source
        if (!blob.isPersistent()) {
            blob = blob.persist();
        }

        DigestInputStream dis = new DigestInputStream(blob.getStream(), md);
        while (dis.available() > 0) {
            dis.read();
        }
        byte[] b = md.digest();
        return Base64.encodeBytes(b);
    }

    public void handleEvent(Event event) throws ClientException {
        if (!initIfNeeded()) {
            return;
        }

        if (!activateDigestComputation) {
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
