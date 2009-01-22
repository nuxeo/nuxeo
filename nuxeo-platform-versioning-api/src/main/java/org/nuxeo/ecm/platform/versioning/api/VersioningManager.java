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

package org.nuxeo.ecm.platform.versioning.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Remote/Local interface for VersioningManager EJB.
 *
 * @author DM
 *
 */
public interface VersioningManager {

    /**
     * Get document incrementation options as defined by versioning rules.
     * Versioning rules might specify that the workflow engine will be
     * interrogated further (this is transparently) and the versioning service
     * will process workflow response afterwards.
     * <p>
     * The flow is like this:
     * <pre>
     *     client
     *       -&gt; query versioning service
     *          [-&gt; query workflow if specified by the rules
     *           &lt;- process wf response ]
     *       &lt;- return list of inc options
     * </pre>
     *
     * @param docModel the document
     * @return a list of version incrementation options available for the given
     *         document ref
     *
     * @throws VersioningException
     * @throws ClientException
     * @throws DocumentException
     */
    VersionIncEditOptions getVersionIncEditOptions(DocumentModel docModel) throws VersioningException,
            ClientException, DocumentException;

    /**
     * Gets the label for the current version of a document, for the UI.
     *
     * @param document the document
     * @return the version label
     * @throws ClientException
     */
    String getVersionLabel(DocumentModel document) throws ClientException;

    /**
     * Returns the property name to use when setting the major version for this
     * document type.
     */
    String getMajorVersionPropertyName(String documentType);

    /**
     * Returns the property name to use when setting the minor version for this
     * document type.
     */
    String getMinorVersionPropertyName(String documentType);

    SnapshotOptions getCreateSnapshotOption(DocumentModel docModel) throws ClientException;

    DocumentModel incrementMinor(DocumentModel docModel) throws ClientException,
            VersioningException;

    DocumentModel incrementMajor(DocumentModel docModel) throws ClientException,
            VersioningException;

    DocVersion getNextVersion(DocumentModel docModel) throws ClientException,
            VersioningException;

}
