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
            + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " + " AND ecm:currentLifeCycleState != 'deleted' ";

    protected String coreSessionId;

    protected String path;

    protected String treeConfigName;

    protected PublishedDocumentFactory factory;

    protected String sid;

    public VirtualCoreFolderPublicationNode(String coreSessionId, String documentPath, String treeConfigName,
            String sid, PublishedDocumentFactory factory) {
        this.coreSessionId = coreSessionId;
        this.path = documentPath;
        this.treeConfigName = treeConfigName;
        this.factory = factory;
        this.sid = sid;
    }

    public String getTitle() {
        return "Sections";
    }

    public String getName() {
        return "sections";
    }

    public PublicationNode getParent() {
        return null;
    }

    public List<PublicationNode> getChildrenNodes() {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
        CoreSession session = getCoreSession();
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
                    childrenNodes.add(new CoreFolderPublicationNode(doc, treeConfigName, sid, this, factory));
                }
            }
        }
        return childrenNodes;
    }

    protected CoreSession getCoreSession() {
        return Framework.getService(CoreSessionService.class).getCoreSession(coreSessionId);
    }

    public List<PublishedDocument> getChildrenDocuments() {
        return Collections.emptyList();
    }

    public String getPath() {
        return path;
    }

    public String getSessionId() {
        return sid;
    }

}
