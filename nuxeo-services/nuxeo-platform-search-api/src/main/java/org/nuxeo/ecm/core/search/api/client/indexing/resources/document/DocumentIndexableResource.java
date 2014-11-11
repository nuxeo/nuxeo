/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: IndexableDocumentInfo.java 13094 2007-03-01 13:36:13Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document;

import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.client.IndexingException;

/**
 * Document Indexable resource.
 * <p>
 * Access indexable data bound to a document model. This class aggregates only
 * the mandatory information needed to access back a document from Nuxeo core.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface DocumentIndexableResource extends NXCoreIndexableResource {

    /**
     * Returns the document UUID.
     *
     * @return the document UUID
     */
    String getDocUUID();

    /**
     * Returns a the document reference.
     *
     * @return a Nuxeo Core DocumenRef instance.
     */
    DocumentRef getDocRef();

    /**
     * Returns the parent document reference.
     * <p>
     * Useful for the backend while returning result items so that DocumentModel
     * can be generated out.
     *
     * @return a Nuxeo Core DocumentRef instance.
     */
    DocumentRef getDocParentRef();

    /**
     * Returns the document type.
     *
     * @return the document type
     */
    String getDocType();

    /**
     * Returns the document path.
     *
     * @return the document path
     */
    Path getDocPath();

    /**
     * Returns the document URL.
     *
     * @return the document URL
     */
    String getDocURL();

    /**
     * Returns the current life cycle state.
     *
     * @return the current life cycle state name.
     */
    String getDocCurrentLifeCycleState();

    /**
     * Returns the merged ACP for the bound document.
     *
     * @return a Nuxeo Core API ACP instance.
     */
    ACP getDocMergedACP() throws IndexingException;

    /**
     * Returns the version label for the bound document.
     *
     * @return the version label for the bound document.
     */
    String getDocVersionLabel();

    /**
     * Returns qualified id.
     *
     * TODO
     *
     * @return the qualified identifier.
     */
    String getQid();

    /**
     * Says if this is a checked in version or not.
     *
     * @return true if the bound document is a version.
     */
    Boolean isDocVersion();

    /**
     * Returns the bound doc name.
     *
     * @return the bound doc name.
     */
    String getDocName();

    /**
     * Is the document a proxy ?
     *
     * @return
     */
    Boolean isDocProxy();

    /**
     * Returns the facets declared by the document model.
     *
     * @return the facets declared by the document model.
     */
    List<String> getDocFacets();

    /**
     * Returns the flags declared on the document model.
     *
     * @return a long value (depending on the DocumentModelImpl for now.
     */
    Long getFlags();

}
