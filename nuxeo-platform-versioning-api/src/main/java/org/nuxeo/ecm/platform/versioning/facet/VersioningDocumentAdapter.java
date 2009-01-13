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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.facet;

import java.text.NumberFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Versioning adapter implementation.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class VersioningDocumentAdapter implements VersioningDocument {

    private static final Log log = LogFactory.getLog(VersioningDocumentAdapter.class);

    private DocumentModel doc;

    private final String majorVersionProperty;

    private final String minorVersionProperty;

    /**
     * Constructor.
     */
    VersioningDocumentAdapter(DocumentModel doc, String propMajorVersion,
            String propMinorVersion) {
        this.doc = doc;
        majorVersionProperty = propMajorVersion;
        minorVersionProperty = propMinorVersion;
    }

    public Long getMajorVersion() throws DocumentException {
        return getValidVersionNumber(majorVersionProperty);
    }

    public Long getMinorVersion() throws DocumentException {
        return getValidVersionNumber(minorVersionProperty);
    }

    public void incrementMajor() throws DocumentException {
        long major = getMajorVersion();
        setMajorVersion(major + 1L);
        setMinorVersion(0L);
    }

    public void incrementMinor() throws DocumentException {
        long minor = getMinorVersion();
        setMinorVersion(minor + 1L);
    }

    public void incrementVersions() {
        // TODO Auto-generated method stub
    }

    public void setMajorVersion(Long value) {
        try {
            doc.setProperty(DocumentModelUtils.getSchemaName(majorVersionProperty),
                    DocumentModelUtils.getFieldName(majorVersionProperty), value);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void setMinorVersion(Long value) {
        try {
            doc.setProperty(DocumentModelUtils.getSchemaName(minorVersionProperty),
                    DocumentModelUtils.getFieldName(minorVersionProperty), value);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    private long getValidVersionNumber(String propName)
            throws DocumentException {
        Object propVal;
        try {
            propVal = doc.getProperty(
                    DocumentModelUtils.getSchemaName(propName),
                    DocumentModelUtils.getFieldName(propName));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

        long ver = 0L;
        if (null == propVal) {
            // versions not initialized
            // could be the case that defaultMajorVersion & defaultMajorVersion
            // are not correctly specifying the properties names for versioning
            log.warn("Versioning field not initialized (property: " + propName
                    + ") for doc: " + doc.getId());
        } else {
            if (!(propVal instanceof Long)) {
                throw new DocumentException("Property " + propName
                        + " should be of type Long");
            }
            ver = (Long) propVal;
        }
        return ver;
    }

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

    public void refetchDoc() throws DocumentException {
        doc = getDocumentModel(doc.getRef(), doc.getRepositoryName());
    }

    private static DocumentModel getDocumentModel(DocumentRef docRef, String repName)
            throws DocumentException {
        CoreSession coreSession = null;
        try {
            // DO NOT: make sure we'll have an authenticated thread
            // the caller might not be authorized
            // LoginContext loginContext = Framework.login()
            // loginContext.login()
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repo = mgr.getRepository(repName);
            coreSession = repo.open();
            return coreSession.getDocument(docRef);
        } catch (Exception e) {
            throw new DocumentException("cannot retrieve document for ref: "
                    + docRef, e);
        } finally {
            if (coreSession != null) {
                CoreInstance.getInstance().close(coreSession);
            }
        }
    }

}
