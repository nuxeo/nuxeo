package org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

/**
 * Java implementation for the marshalled {@link PublishedDocument}
 *
 * @author tiry
 */
public class BasicPublishedDocument implements PublishedDocument {

    private DocumentRef docRef;

    private String repositoryName;

    private String serverName;

    private String versionLabel;

    private String path;

    private String parentPath;

    private boolean isPending;

    private static final long serialVersionUID = 1L;

    public BasicPublishedDocument(DocumentRef docRef, String repositoryName,
            String serverName, String versionLabel, String path,
            String parentPath, boolean isPending) {
        this.docRef = docRef;
        this.repositoryName = repositoryName;
        this.serverName = serverName;
        this.versionLabel = versionLabel;
        this.path = path;
        this.parentPath = parentPath;
        this.isPending = isPending;
    }

    public DocumentRef getSourceDocumentRef() {
        return docRef;
    }

    public String getSourceRepositoryName() {
        return repositoryName;
    }

    public String getSourceServer() {
        return serverName;
    }

    public String getSourceVersionLabel() {
        return versionLabel;
    }

    public String getPath() {
        return path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public boolean isPending() {
        return isPending;
    }

    public Type getType() {
        return Type.REMOTE;
    }

}
