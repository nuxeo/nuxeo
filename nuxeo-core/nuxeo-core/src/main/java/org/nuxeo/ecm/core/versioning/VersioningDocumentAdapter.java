/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.versioning;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter showing the versioning aspects of documents.
 */
public class VersioningDocumentAdapter implements VersioningDocument {

    public final DocumentModel doc;

    public final VersioningService service;

    public VersioningDocumentAdapter(DocumentModel doc) {
        try {
            service = Framework.getService(VersioningService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.doc = doc;
    }

    @Override
    public Long getMajorVersion() throws DocumentException {
        return Long.valueOf(getValidVersionNumber(VersioningService.MAJOR_VERSION_PROP));
    }

    @Override
    public Long getMinorVersion() throws DocumentException {
        return Long.valueOf(getValidVersionNumber(VersioningService.MINOR_VERSION_PROP));
    }

    @Override
    public String getVersionLabel() {
        return service.getVersionLabel(doc);
    }

    private long getValidVersionNumber(String propName) {
        Object propVal;
        try {
            propVal = doc.getPropertyValue(propName);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return (propVal == null || !(propVal instanceof Long)) ? 0
                : ((Long) propVal).longValue();
    }

}
