/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.relations.api.*;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class PublicationRelationHelper {

    public static final String PUBLICATION_GRAPH_NAME = "publication";

    public static final String PUBLICATION_TREE_NAMESPACE = "http://www.nuxeo.org/publication/tree/";

    public static final Resource PUBLISHED_BY = new ResourceImpl(
            "http://www.nuxeo.org/publication/publishedBy");

    private static Log log = LogFactory.getLog(PublicationRelationHelper.class);

    private PublicationRelationHelper() {
        // Helper class
    }

    public static void addPublicationRelation(DocumentModel documentModel,
            PublicationTree publicationTree) throws ClientException {
        RelationManager rm = RelationHelper.getRelationManager();
        QNameResource docResource = RelationHelper.getDocumentResource(documentModel);
        QNameResource treeResource = (QNameResource) rm.getResource(PUBLICATION_TREE_NAMESPACE,
                publicationTree, null);
        Statement stmt = new StatementImpl(docResource, PUBLISHED_BY,
                treeResource);
        rm.add(PUBLICATION_GRAPH_NAME, Collections.singletonList(stmt));
    }

    /**
     *
     * @param documentModel
     * @throws ClientException
     */
    public static void removePublicationRelation(DocumentModel documentModel)
            throws ClientException {
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel,
                PUBLISHED_BY);
        RelationManager rm = RelationHelper.getRelationManager();
        if (stmts != null) {
            rm.remove(PUBLICATION_GRAPH_NAME, stmts);
        }
    }

    /**
     *
     * @param documentModel
     * @return
     */
    public static boolean isPublished(DocumentModel documentModel) {
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel,
                PUBLISHED_BY);
        return stmts != null && !stmts.isEmpty();
    }

    /**
     *
     * @param documentModel
     * @return
     * @throws ClientException
     */
    public static PublicationTree getPublicationTreeUsedForPublishing(
            DocumentModel documentModel, CoreSession coreSession)
            throws ClientException {
        if (!isPublished(documentModel)) {
            throw new ClientException("The document "
                    + documentModel.getPathAsString()
                    + " is not a published document");
        }
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel,
                PUBLISHED_BY);
        Statement statement = stmts.get(0);

        Node node = statement.getObject();
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) statement.getObject();
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,
                    coreSession.getSessionId());

            RelationManager rm = RelationHelper.getRelationManager();
            return (PublicationTree) rm.getResourceRepresentation(
                    resource.getNamespace(), resource, context);
        } else {
            log.error("Resource is not a QNameResource, check the namespace");
            return null;
        }
    }

}
