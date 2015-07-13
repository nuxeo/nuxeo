package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

public class EmptyRoot extends CoreFolderPublicationNode {

    private static final long serialVersionUID = 1L;

    public EmptyRoot(String treeConfigName, String sid, PublishedDocumentFactory factory) {
        super(null, treeConfigName, sid, factory);
    }

    @SuppressWarnings("unchecked")
    public List<PublishedDocument> getChildrenDocuments() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<PublicationNode> getChildrenNodes() {
        return Collections.emptyList();
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getTitle() {
        return "";
    }

}
