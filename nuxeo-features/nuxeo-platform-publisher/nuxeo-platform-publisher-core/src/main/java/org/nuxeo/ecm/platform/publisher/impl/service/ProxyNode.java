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

package org.nuxeo.ecm.platform.publisher.impl.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Remotable implementation of the {@link PublicationNode} interface. Because
 * some {@link PublicationNode} implementation may be bound to local resources
 * (network connexions, local filesystem ...) all {@link PublicationNode}
 * returned by the service are wrapped into this RemotablePublicationNode.
 *
 * @author tiry
 */
public class ProxyNode extends AbstractRemotableNode implements PublicationNode {

    private static final long serialVersionUID = 1L;

    protected String nodeType;

    protected String nodeLabel;

    protected String nodePath;

    protected String nodeName;

    public ProxyNode(PublicationNode node, String sid) throws ClientException {
        nodeType = node.getNodeType();
        nodeLabel = node.getTitle();
        nodePath = node.getPath();
        treeName = node.getTreeConfigName();
        nodeName = node.getName();
        sessionId = sid;
    }

    @Override
    protected RemotePublicationTreeManager getPublisher()
            throws ClientException {
        if (service == null) {
            try {
                service = Framework.getService(RemotePublicationTreeManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return service;
    }

    public String getTitle() {
        return nodeLabel;
    }

    public String getName() {
        return nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getPath() {
        return nodePath;
    }

    public String getTreeConfigName() {
        return treeName;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    protected String getServerTreeSessionId() {
        return getSessionId();
    }

    @Override
    protected String getTargetTreeName() {
        return treeName;
    }

    @Override
    protected PublicationNode switchToServerNode(PublicationNode node)
            throws ClientException {
        // no wrap
        return node;
    }

    @Override
    protected PublicationNode switchToClientNode(PublicationNode node)
            throws ClientException {
        // no wrap
        return node;
    }

}
