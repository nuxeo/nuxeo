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

import java.text.NumberFormat;

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

    @Override
    @Deprecated
    public void incrementMajor() throws DocumentException {
        long major = getValidVersionNumber(VersioningService.MAJOR_VERSION_PROP);
        setMajorVersion(Long.valueOf(major + 1));
        setMinorVersion(Long.valueOf(0));
    }

    @Override
    @Deprecated
    public void incrementMinor() throws DocumentException {
        long minor = getValidVersionNumber(VersioningService.MINOR_VERSION_PROP);
        setMinorVersion(Long.valueOf(minor + 1));
    }

    @Override
    @Deprecated
    public void setMajorVersion(Long value) {
        try {
            doc.setPropertyValue(VersioningService.MAJOR_VERSION_PROP, value);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    @Deprecated
    public void setMinorVersion(Long value) {
        try {
            doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP, value);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
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

    @Override
    @Deprecated
    public void refetchDoc() throws DocumentException {
        try {
            doc.refresh();
        } catch (ClientException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    @Deprecated
    public void incrementVersions() {
        // does nothing
    }

    @Override
    @Deprecated
    public String getVersionAsString(int majorDigits, int minorDigits, char sep)
            throws DocumentException {
        StringBuilder buf = new StringBuilder();
        NumberFormat fmt = getFmt(majorDigits);
        buf.append(fmt.format(getMajorVersion()));
        buf.append(sep);
        NumberFormat fmt2 = getFmt(minorDigits);
        buf.append(fmt2.format(getMinorVersion()));
        return buf.toString();
    }

    private static NumberFormat getFmt(int digits) {
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumIntegerDigits(digits);
        fmt.setMinimumIntegerDigits(digits);
        fmt.setMaximumFractionDigits(0);
        return fmt;
    }

}
