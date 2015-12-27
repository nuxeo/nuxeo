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

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

import java.util.List;

/**
 * Java implementation for the marshaled {@link PublicationNode}.
 *
 * @author tiry
 */
public class BasicPublicationNode implements PublicationNode {

    private static final long serialVersionUID = 1L;

    protected String nodeType;

    protected String nodePath;

    protected String nodeTitle;

    protected String treeName;

    protected String sid;

    public BasicPublicationNode(String nodeType, String nodePath, String nodeTitle, String treeName) {
        this(nodeType, nodePath, nodeTitle, treeName, null);
    }

    public BasicPublicationNode(String nodeType, String nodePath, String nodeTitle, String treeName, String sid) {
        this.nodePath = nodePath;
        this.nodeType = nodeType;
        this.nodeTitle = nodeTitle;
        this.treeName = treeName;
        this.sid = sid;
    }

    public List<PublishedDocument> getChildrenDocuments() {
        throw new NuxeoException("Can not be called on a remote node");
    }

    public List<PublicationNode> getChildrenNodes() {
        throw new NuxeoException("Can not be called on a remote node");
    }

    public String getNodeType() {
        return nodeType;
    }

    public PublicationNode getParent() {
        return null;
    }

    public String getPath() {
        return nodePath;
    }

    public String getTitle() {
        return nodeTitle;
    }

    public String getName() {
        if (nodePath == null) {
            return null;
        }
        return new Path(nodePath).lastSegment();
    }

    public String getTreeConfigName() {
        return treeName;
    }

    public String getSessionId() {
        return sid;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

}
