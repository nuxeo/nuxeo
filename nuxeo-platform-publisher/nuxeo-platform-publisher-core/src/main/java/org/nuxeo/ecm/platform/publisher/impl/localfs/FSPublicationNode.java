package org.nuxeo.ecm.platform.publisher.impl.localfs;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FSPublicationNode extends AbstractPublicationNode implements
        PublicationNode {

    private static final long serialVersionUID = 1L;

    protected File folder;

    protected String sid;

    public FSPublicationNode(String path, String treeName, String sid) {
        this(new File(path), treeName, sid);
    }

    public FSPublicationNode(File folder, String treeName, String sid) {
        if (!folder.exists()) {
            throw new IllegalArgumentException("Root publication folder does not exist");
        }
        this.folder = folder;
        this.treeName = treeName;
        this.sid = sid;
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {

        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();
        List<File> children = Arrays.asList(folder.listFiles());
        Collections.sort(children);
        for (File child : children) {
            if (!child.isDirectory()) {
                try {
                    childrenDocs.add(new FSPublishedDocument(child));
                } catch (NotFSPublishedDocumentException e) {
                    throw new ClientException(
                            "Error whild creating PublishedDocument from file",
                            e);
                }
            }
        }
        return childrenDocs;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
        List<File> children = Arrays.asList(folder.listFiles());
        Collections.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                childrenNodes.add(new FSPublicationNode(child,
                        getTreeConfigName(), sid));
            }
        }
        return childrenNodes;
    }

    public String getName() throws ClientException {
        return folder.getName();
    }

    public PublicationNode getParent() {
        String parentPath = new Path(getPath()).removeLastSegments(1).toString();
        File parentFolder = new File(parentPath);
        return new FSPublicationNode(parentFolder, getTreeConfigName(), sid);
    }

    public String getPath() {
        return folder.getAbsolutePath();
    }

    public String getTitle() throws ClientException {
        return getName();
    }

    public String getSessionId() {
        return sid;
    }

}
