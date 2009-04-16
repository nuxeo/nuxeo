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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.versioning.facet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter class factory for Versioning Document interface.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class VersioningAdapterFactory implements DocumentAdapterFactory {

    private static final Log log = LogFactory.getLog(VersioningAdapterFactory.class);

    public Object getAdapter(DocumentModel doc, Class itf) {
        VersioningManager vservice = Framework.getLocalService(
                VersioningManager.class);
        if (null == vservice) {
            try {
                vservice = Framework.getService(VersioningManager.class);
            } catch (Exception e) {
                log.error("Error getting facade to VersioningService", e);
            }
        }

        if (null == vservice) {
            log.error("Cannot create VersioningDocumentAdapter. "
                    + VersioningManager.class.getSimpleName()
                    + " service not available. ");
            return null;
        }

        final String documentType = doc.getType();

        final String fieldNameMajorVersion = vservice.getMajorVersionPropertyName(documentType);
        final String fieldNameMinorVersion = vservice.getMinorVersionPropertyName(documentType);

        return new VersioningDocumentAdapter(doc, fieldNameMajorVersion,
                fieldNameMinorVersion);
    }

}
