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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

public class DigestComputer extends AbstractEventListener {

    private Boolean initDone = false;

    private List<String> xpathFields;

    private String digestAlgo = "sha-256";

    private Boolean activateDigestComputation = false;

    private static final Log log = LogFactory.getLog(DigestComputer.class);

    private Boolean initIfNeeded() {
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

    @Override
    public void notifyEvent(CoreEvent coreEvent) throws Exception {
        if (!initIfNeeded()) {
            return;
        }

        if (!activateDigestComputation) {
            return;
        }

        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) source;
            if (doc.isProxy()) {
                return;
            }
            String evt = coreEvent.getEventId();
            if (DocumentEventTypes.ABOUT_TO_CREATE.equals(evt)
                    || DocumentEventTypes.BEFORE_DOC_UPDATE.equals(evt)) {
                addDigestToDocument(doc);
            }
        }
    }

    private void addDigestToDocument(DocumentModel doc) {
        for (String xpathField : xpathFields) {
            Property blobProp = null;
            try {
                blobProp = doc.getProperty(xpathField);
            } catch (PropertyException e) {
                log.debug("Property " + xpathField
                        + " not found on doc, skipping");
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

}
