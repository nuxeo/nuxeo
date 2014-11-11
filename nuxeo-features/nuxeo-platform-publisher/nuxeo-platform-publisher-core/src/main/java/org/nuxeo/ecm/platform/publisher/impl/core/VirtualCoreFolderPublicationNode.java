package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class VirtualCoreFolderPublicationNode extends AbstractPublicationNode {

    protected static String ACCESSIBLE_CHILDREN_QUERY = "SELECT * FROM Document"
            + " WHERE ecm:primaryType = 'Section' AND ecm:path STARTSWITH '%s'"
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
            String query = String.format(ACCESSIBLE_CHILDREN_QUERY, path);
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
