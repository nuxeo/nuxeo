/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class PublicationRelationHelper {

    public static final String PUBLICATION_GRAPH_NAME = "publication";

    public static final String PUBLICATION_TREE_NAMESPACE = "http://www.nuxeo.org/publication/tree/";

    public static final Resource PUBLISHED_BY = new ResourceImpl("http://www.nuxeo.org/publication/publishedBy");

    private static Log log = LogFactory.getLog(PublicationRelationHelper.class);

    private PublicationRelationHelper() {
        // Helper class
    }

    public static void addPublicationRelation(DocumentModel documentModel, PublicationTree publicationTree)
            {
        RelationManager rm = RelationHelper.getRelationManager();
        QNameResource docResource = RelationHelper.getDocumentResource(documentModel);
        QNameResource treeResource = new QNameResourceImpl(PUBLICATION_TREE_NAMESPACE, publicationTree.getConfigName());
        Statement stmt = new StatementImpl(docResource, PUBLISHED_BY, treeResource);
        rm.getGraphByName(PUBLICATION_GRAPH_NAME).add(stmt);
    }

    public static void removePublicationRelation(DocumentModel documentModel) {
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel, PUBLISHED_BY);
        RelationManager rm = RelationHelper.getRelationManager();
        if (stmts != null) {
            rm.getGraphByName(PUBLICATION_GRAPH_NAME).remove(stmts);
        }
    }

    public static boolean isPublished(DocumentModel documentModel) {
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel, PUBLISHED_BY);
        return stmts != null && !stmts.isEmpty();
    }

    public static PublicationTree getPublicationTreeUsedForPublishing(DocumentModel documentModel,
            CoreSession coreSession) {
        if (!isPublished(documentModel)) {
            throw new NuxeoException("The document " + documentModel.getPathAsString()
                    + " is not a published document");
        }
        List<Statement> stmts = RelationHelper.getStatements(PUBLICATION_GRAPH_NAME, documentModel, PUBLISHED_BY);
        Statement statement = stmts.get(0);

        PublicationTree tree = null;
        Node node = statement.getObject();
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) statement.getObject();
            String localName = resource.getLocalName();
            PublisherService publisherService = Framework.getService(PublisherService.class);
            tree = publisherService.getPublicationTree(localName, coreSession, null);
        } else {
            log.error("Resource is not a QNameResource, check the namespace");

        }
        return tree;
    }

}
