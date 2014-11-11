/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
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

    public BasicPublicationNode(String nodeType, String nodePath,
            String nodeTitle, String treeName) {
        this(nodeType, nodePath, nodeTitle, treeName, null);
    }

    public BasicPublicationNode(String nodeType, String nodePath,
            String nodeTitle, String treeName, String sid) {
        this.nodePath = nodePath;
        this.nodeType = nodeType;
        this.nodeTitle = nodeTitle;
        this.treeName = treeName;
        this.sid = sid;
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        throw new ClientException("Can not be called on a remote node");
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        throw new ClientException("Can not be called on a remote node");
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
