package org.nuxeo.ecm.platform.publisher.synchonize.server;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

import java.util.List;

public interface ServerSynchronizablePublicationTree extends PublicationTree {

    List<PublicationNode> listModifiedNodes(long timeDelta);

    List<PublishedDocument> listModifiedPublishedDocuments(long timeDelta);

    String exportPublishedDocumentByPath(String path);

}
