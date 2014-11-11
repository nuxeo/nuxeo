package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

public class EmptyRoot extends CoreFolderPublicationNode {

    private static final long serialVersionUID = 1L;

    public EmptyRoot(String treeConfigName, String sid,
            PublishedDocumentFactory factory) throws ClientException {
        super(null, treeConfigName, sid, factory);
    }

    @SuppressWarnings("unchecked")
    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<PublicationNode> getChildrenNodes() throws ClientException {
        return Collections.emptyList();
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    public String getName() throws ClientException {
        return "";
    }

    @Override
    public String getTitle() throws ClientException {
        return "";
    }

}
