/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */

package org.nuxeo.documentrouting.web.relations;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Retrieves relations for current document route
 * 
 * @author Mariana Cedica
 */
@Name("docRoutingRelationActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class DocumentRoutingRelationActionsBean extends
        DocumentContextBoundActionBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final String CURRENT_DOC_ROUTING_RELATION_SEARCH = "CURRENT_DOC_ROUTING_RELATION_SEARCH";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public DocumentModel getDocumentModel(String id) throws ClientException {
        return documentManager.getDocument(new IdRef(id));
    }

    public List<DocumentModel> getDocumentRelationSuggestions(Object input)
            throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        try {
            QueryModelService qms = Framework.getService(QueryModelService.class);
            if (qms == null) {
                return docs;
            }

            QueryModelDescriptor qmDescriptor = qms.getQueryModelDescriptor(CURRENT_DOC_ROUTING_RELATION_SEARCH);
            if (qmDescriptor == null) {
                return docs;
            }

            List<Object> queryParams = new ArrayList<Object>();
            queryParams.add(0, computePathWorkspaceRoot());
            queryParams.add(1, String.format("%s%%", input));
            QueryModel qm = new QueryModel(qmDescriptor);
            docs = qm.getDocuments(documentManager, queryParams.toArray());
        } catch (Exception e) {
            throw new ClientException("error searching for documents", e);
        }
        return docs;
    }

    protected String computePathWorkspaceRoot() throws ClientException {
        DocumentModel doc = documentManager.getRootDocument();
        doc = documentManager.getChildren(doc.getRef(), "Domain").get(0);
        doc = documentManager.getChildren(doc.getRef(), "WorkspaceRoot").get(0);
        return doc.getPathAsString();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        // TODO Auto-generated method stub
    }

}
