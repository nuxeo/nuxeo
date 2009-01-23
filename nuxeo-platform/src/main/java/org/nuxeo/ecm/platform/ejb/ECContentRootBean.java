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

package org.nuxeo.ecm.platform.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.interfaces.ejb.ECContentRoot;
import org.nuxeo.ecm.platform.interfaces.local.ECContentRootLocal;

/**
 * Workspace implementation class.
 *
 * @author Razvan Caraghin
 *
 */
@Stateless
public class ECContentRootBean implements ECContentRoot, ECContentRootLocal {

    private static final Log log = LogFactory.getLog(ECContentRootBean.class);

    public void remove() {}

    public List<DocumentModel> getContentRootChildren(String documentType,
            DocumentRef docRef, CoreSession handle) throws ClientException {
        FacetFilter facetFilter = new FacetFilter("HiddenInNavigation", false);
        try {
            assert null != docRef;
            assert null != handle;

            log.debug("Making call to get the children for doc id:" + docRef);
            List<DocumentModel> children = handle.getChildren(docRef, null,
                    SecurityConstants.READ, facetFilter, null);
            assert null != children;
            List<DocumentModel> contentRootChildren = null;

            // retrieve the documents from the domain associated with the
            // content
            // root specified by the passed documentType
            for (DocumentModel indexDocumentModel : children) {
                // XXX AT: ugly hack, this should not depend on the document schema
                if (documentType.equalsIgnoreCase((String) indexDocumentModel
                        .getProperty("dublincore", "description"))) {
                    log.debug("Making call to get the children for doc id:"
                            + docRef);
                    contentRootChildren = handle.getChildren(
                            indexDocumentModel.getRef(), null,
                            SecurityConstants.READ,
                            facetFilter, null);
                    break;
                }
            }

            // content root
            List<DocumentModel> filteredDocuments = new ArrayList<DocumentModel>();

            if (null != contentRootChildren) {
                for (DocumentModel document : contentRootChildren) {
                    if (document.getType().equalsIgnoreCase(documentType)) {
                        filteredDocuments.add(document);
                    }
                }
            }

            return filteredDocuments;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<DocumentModel> getContentRootDocuments(DocumentRef docRef,
            CoreSession handle) throws ClientException {
        FacetFilter facetFilter = new FacetFilter("HiddenInNavigation", false);
        try {
            assert null != docRef;
            assert null != handle;

            log.debug("Making call to get the children for doc id:" + docRef);
            List<DocumentModel> contentRootDocuments = handle
                    .getChildren(docRef, null, SecurityConstants.READ, facetFilter, null);
            assert null != contentRootDocuments;

            return contentRootDocuments;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

}
