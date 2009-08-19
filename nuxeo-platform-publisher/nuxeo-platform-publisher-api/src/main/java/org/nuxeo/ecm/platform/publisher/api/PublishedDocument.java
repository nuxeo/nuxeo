package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.DocumentRef;

import java.io.Serializable;

/**
 * 
 * Interface of a Document that was published into a PublicationNode
 * 
 * @author tiry
 * 
 */
public interface PublishedDocument extends Serializable {

    DocumentRef getSourceDocumentRef();

    String getSourceRepositoryName();

    String getSourceServer();

    String getSourceVersionLabel();

    String getPath();

    String getParentPath();

    /**
     * Returns {@code true} if this document is waiting approval, {@code false}
     * otherwise.
     */
    boolean isPending();

}
