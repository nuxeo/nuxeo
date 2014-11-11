package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * Interface for a Node inside the publication tree. The Node is abstract, the
 * implemenation could be : a Core Folder, a FileSystem directory, a Folder on a
 * remote core ...
 * 
 * @author tiry
 * 
 */
public interface PublicationNode extends Serializable {

    /**
     * get Title of the Node
     * 
     * @return
     * @throws ClientException
     */
    String getTitle() throws ClientException;

    /**
     * get the name of the Node
     * 
     * @return
     * @throws ClientException
     */
    String getName() throws ClientException;

    PublicationNode getParent();

    List<PublicationNode> getChildrenNodes() throws ClientException;

    List<PublishedDocument> getChildrenDocuments() throws ClientException;

    String getNodeType();

    String getType();

    String getPath();

    String getTreeConfigName();

    String getSessionId();

}
