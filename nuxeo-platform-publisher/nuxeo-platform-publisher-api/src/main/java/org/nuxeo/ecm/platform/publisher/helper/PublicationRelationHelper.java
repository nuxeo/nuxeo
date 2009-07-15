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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.relations.api.*;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

import java.util.List;
import java.util.Collections;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class PublicationRelationHelper {

    public static final Resource PUBLISHED_BY = new ResourceImpl(
            "http://www.nuxeo.org/publication/publishedBy");

    private PublicationRelationHelper() {
        // Helper class
    }

    public static void addPublicationRelation(DocumentModel documentModel, String treeName) throws ClientException {
        QNameResource docResource = RelationHelper.getDocumentResource(documentModel);
        Statement stmt = new StatementImpl(docResource,
                    PUBLISHED_BY, new LiteralImpl(treeName));
        RelationManager rm = RelationHelper.getRelationManager();
        rm.add(RelationConstants.GRAPH_NAME, Collections.singletonList(stmt));
    }

    public static void removePublicationRelation(DocumentModel documentModel) throws ClientException {
        List<Statement> stmts = RelationHelper.getStatements(documentModel, PUBLISHED_BY);
        RelationManager rm = RelationHelper.getRelationManager();
        if (stmts != null) {
            rm.remove(RelationConstants.GRAPH_NAME, stmts);
        }
    }

    public static boolean isPublished(DocumentModel documentModel) {
        List<Statement> stmts = RelationHelper.getStatements(documentModel, PUBLISHED_BY);
        return !stmts.isEmpty();
    }

    public static String getTreeNameUsedForPublishing(DocumentModel documentModel) throws ClientException {
        if (!isPublished(documentModel)) {
            throw new ClientException("This document " + documentModel.getPathAsString() +  " is not a published document");
        }
        List<Statement> stmts = RelationHelper.getStatements(documentModel, PUBLISHED_BY);
        Statement statement = stmts.get(0);
        return ((Literal) statement.getObject()).getValue();
    }

}
