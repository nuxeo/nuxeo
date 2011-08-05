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

package org.nuxeo.ecm.platform.publisher.remoting.client;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.impl.service.AbstractRemotableNode;
import org.nuxeo.ecm.platform.publisher.impl.service.ProxyNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;

/**
 * {@link PublicationNode} implementation that points to a remote tree on a
 * remote server.
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

    public ClientRemotePublicationNode(String treeConfigName, String sid,
            PublicationNode node, String serverSessionId,
            RemotePublicationTreeManager service, String targetTreeName)
            throws ClientException {
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
    protected PublicationNode switchToServerNode(PublicationNode node)
            throws ClientException {

        if (node instanceof ClientRemotePublicationNode) {
            ClientRemotePublicationNode cNode = (ClientRemotePublicationNode) node;
            return new BasicPublicationNode(cNode.getNodeType(),
                    cNode.getPath(), cNode.getTitle(),
                    cNode.getUnwrappedTreeName(), serverSessionId);
        }
        if (node instanceof ProxyNode) {
            ProxyNode rNode = (ProxyNode) node;
            return new BasicPublicationNode(rNode.getNodeType(),
                    rNode.getPath(), rNode.getTitle(), getTargetTreeName(),
                    serverSessionId);
        } else {
            return node;
        }

        // return new ClientRemotePublicationNode(treeName, sessionId,node,
        // serverSessionId,
        // service, getTargetTreeName());
    }

    @Override
    protected PublicationNode switchToClientNode(PublicationNode node)
            throws ClientException {
        return new ClientRemotePublicationNode(treeName, sessionId, node,
                serverSessionId, service, getTargetTreeName());
    }

    @Override
    protected String getServerTreeSessionId() {
        return serverSessionId;
    }

    @Override
    protected RemotePublicationTreeManager getPublisher()
            throws ClientException {
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
