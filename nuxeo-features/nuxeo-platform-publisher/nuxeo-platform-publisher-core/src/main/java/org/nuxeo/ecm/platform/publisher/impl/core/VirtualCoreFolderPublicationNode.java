/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class VirtualCoreFolderPublicationNode extends AbstractPublicationNode {

    private static final long serialVersionUID = 1L;

    protected static String ACCESSIBLE_CHILDREN_QUERY = "SELECT * FROM Document"
            + " WHERE ecm:primaryType = 'Section' AND ecm:path STARTSWITH %s"
            + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
            + " AND ecm:currentLifeCycleState != 'deleted' ";

    protected String coreSessionId;

    protected String path;

    protected String treeConfigName;

    protected PublishedDocumentFactory factory;

    protected String sid;

    public VirtualCoreFolderPublicationNode(String coreSessionId,
            String documentPath, String treeConfigName, String sid,
            PublishedDocumentFactory factory) {
        this.coreSessionId = coreSessionId;
        this.path = documentPath;
        this.treeConfigName = treeConfigName;
        this.factory = factory;
        this.sid = sid;
    }

    public String getTitle() throws ClientException {
        return "Sections";
    }

    public String getName() throws ClientException {
        return "sections";
    }

    public PublicationNode getParent() {
        return null;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
        CoreSession session = getCoreSession();
        if (session != null) {
            String query = String.format(ACCESSIBLE_CHILDREN_QUERY,
                    NXQL.escapeString(path));
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
                    childrenNodes.add(new CoreFolderPublicationNode(doc,
                            treeConfigName, sid, this, factory));
                }
            }
        }
        return childrenNodes;
    }

    protected CoreSession getCoreSession() {
        return CoreInstance.getInstance().getSession(coreSessionId);
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        return Collections.emptyList();
    }

    public String getPath() {
        return path;
    }

    public String getSessionId() {
        return sid;
    }

}
