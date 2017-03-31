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

package org.nuxeo.ecm.platform.publisher.api;


import java.io.Serializable;
import java.util.List;

/**
 * Interface for a Node inside the publication tree. The Node is abstract, the implementation could be : a Core Folder,
 * a FileSystem directory, a Folder on a remote core ...
 *
 * @author tiry
 */
public interface PublicationNode extends Serializable {

    String getTitle();

    String getName();

    PublicationNode getParent();

    List<PublicationNode> getChildrenNodes();

    List<PublishedDocument> getChildrenDocuments();

    String getNodeType();

    String getType();

    String getPath();

    PublicationTree getTree();

}
