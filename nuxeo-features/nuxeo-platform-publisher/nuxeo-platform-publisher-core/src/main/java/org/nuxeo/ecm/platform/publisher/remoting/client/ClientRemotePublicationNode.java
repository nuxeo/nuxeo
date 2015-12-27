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

package org.nuxeo.ecm.platform.publisher.remoting.client;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.impl.service.AbstractRemotableNode;
import org.nuxeo.ecm.platform.publisher.impl.service.ProxyNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;

/**
 * {@link PublicationNode} implementation that points to a remote tree on a remote server.
 *
 * @author tiry
 */
public class ClientRemotePublicationNode extends AbstractRemotableNode {

    private static final long serialVersionUID = 1L;

    protected String targetTreeName;

    protected String name;

    protected String serverSessionId;

    protected String path;

    protected String nodeType;

    protected String nodeTitle;

    protected String treeName;

    public ClientRemotePublicationNode(String treeConfigName, String sid, PublicationNode node, String serverSessionId,
            RemotePublicationTreeManager service, String targetTreeName) {
        this.serverSessionId = serverSessionId;
        this.sessionId = sid;
        this.service = service;
        this.path = node.getPath();
        this.nodeType = node.getNodeType();
        this.nodeTitle = node.getTitle();
        this.treeName = treeConfigName;
        this.targetTreeName = targetTreeName;
        this.name = node.getName();
    }

    @Override
    protected PublicationNode switchToServerNode(PublicationNode node) {

        if (node instanceof ClientRemotePublicationNode) {
            ClientRemotePublicationNode cNode = (ClientRemotePublicationNode) node;
            return new BasicPublicationNode(cNode.getNodeType(), cNode.getPath(), cNode.getTitle(),
                    cNode.getUnwrappedTreeName(), serverSessionId);
        }
        if (node instanceof ProxyNode) {
            ProxyNode rNode = (ProxyNode) node;
            return new BasicPublicationNode(rNode.getNodeType(), rNode.getPath(), rNode.getTitle(),
                    getTargetTreeName(), serverSessionId);
        } else {
            return node;
        }

        // return new ClientRemotePublicationNode(treeName, sessionId,node,
        // serverSessionId,
        // service, getTargetTreeName());
    }

    @Override
    protected PublicationNode switchToClientNode(PublicationNode node) {
        return new ClientRemotePublicationNode(treeName, sessionId, node, serverSessionId, service, getTargetTreeName());
    }

    @Override
    protected String getServerTreeSessionId() {
        return serverSessionId;
    }

    @Override
    protected RemotePublicationTreeManager getPublisher() {
        return service;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return nodeTitle;
    }

    public String getName() {
        return name;
    }

    public String getTreeConfigName() {
        return treeName;
    }

    @Override
    protected String getTargetTreeName() {
        return targetTreeName;
    }

    public String getUnwrappedTreeName() {
        return getTargetTreeName();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRemoteSessionId() {
        return serverSessionId;
    }
}
