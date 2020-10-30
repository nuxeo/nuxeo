/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class VirtualCoreFolderPublicationNode extends AbstractPublicationNode {

    private static final long serialVersionUID = 1L;

    protected static String ACCESSIBLE_CHILDREN_QUERY = "SELECT * FROM Document"
            + " WHERE ecm:primaryType = 'Section' AND ecm:path STARTSWITH %s"
            + " AND ecm:isVersion = 0 AND ecm:isProxy = 0 " + " AND ecm:isTrashed = 0 ";

    protected CoreSession coreSession;

    protected String path;

    protected PublishedDocumentFactory factory;

    public VirtualCoreFolderPublicationNode(CoreSession coreSession, String documentPath, PublicationTree tree,
            PublishedDocumentFactory factory) {
        super(tree);
        this.coreSession = coreSession;
        this.path = documentPath;
        this.factory = factory;
    }

    @Override
    public String getTitle() {
        return "Sections";
    }

    @Override
    public String getName() {
        return "sections";
    }

    @Override
    public PublicationNode getParent() {
        return null;
    }

    @Override
    public List<PublicationNode> getChildrenNodes() {
        List<PublicationNode> childrenNodes = new ArrayList<>();
        CoreSession session = coreSession;
        if (session != null) {
            String query = String.format(ACCESSIBLE_CHILDREN_QUERY, NXQL.escapeString(path));
            List<DocumentModel> docs = session.query(query);
            for (DocumentModel doc : docs) {
                Path path = doc.getPath().removeLastSegments(1);
                boolean foundParent = false;
                for (DocumentModel d : docs) {
                    if (d.getPath().equals(path)) {
                        foundParent = true;
                    }
                }
                if (!foundParent) {
                    childrenNodes.add(new CoreFolderPublicationNode(doc, tree, this, factory));
                }
            }
        }
        return childrenNodes;
    }

    @Override
    public List<PublishedDocument> getChildrenDocuments() {
        return Collections.emptyList();
    }

    @Override
    public String getPath() {
        return path;
    }

}
