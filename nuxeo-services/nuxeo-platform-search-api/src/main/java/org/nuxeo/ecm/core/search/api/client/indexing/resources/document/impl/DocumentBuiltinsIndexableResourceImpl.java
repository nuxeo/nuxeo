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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentBuiltinsIndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentBuiltinsIndexableResourceImpl extends
        DocumentIndexableResourceImpl implements
        DocumentBuiltinsIndexableResource {

    private static final long serialVersionUID = 7358476074067896797L;

    private static final Log log = LogFactory.getLog(DocumentBuiltinsIndexableResource.class);

    public DocumentBuiltinsIndexableResourceImpl() {
    }

    public DocumentBuiltinsIndexableResourceImpl(DocumentModel dm,
            IndexableResourceConf conf, String sid) {
        super(dm, conf, sid);
    }

    @Override
    public Serializable getValueFor(String indexableDataName)
            throws IndexingException {

        Serializable value = null;

        if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_PARENT_REF)) {
            value = getDocParentRef();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_REF)) {
            value = getDocRef();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_TYPE)) {
            value = getDocType();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_PATH)) {
            value = getDocPath();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE)) {
            value = getDocCurrentLifeCycleState();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME)) {
            value = getDocRepositoryName();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_URL)) {
            value = getDocURL();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL)) {
            value = getDocVersionLabel();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_IS_CHECKED_IN_VERSION)) {
            value = isDocVersion();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_NAME)) {
            value = getDocName();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_QID)) {
            value = getQid();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_UUID)) {
            value = getDocUUID();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_IS_PROXY)) {
            value = isDocProxy();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_FACETS)) {
            value = (Serializable) getDocFacets();
        } else if (indexableDataName.equals(BuiltinDocumentFields.FIELD_DOC_FLAGS)) {
            value = getFlags();
        }

        log.debug("Indexing builtin : " + indexableDataName + " : " + value);

        return value;
    }

}
