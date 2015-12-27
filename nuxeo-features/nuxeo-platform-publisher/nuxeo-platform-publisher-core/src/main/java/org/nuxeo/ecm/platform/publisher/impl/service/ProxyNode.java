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

package org.nuxeo.ecm.platform.publisher.impl.service;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Remotable implementation of the {@link PublicationNode} interface. Because some {@link PublicationNode}
 * implementation may be bound to local resources (network connexions, local filesystem ...) all {@link PublicationNode}
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

    public ProxyNode(PublicationNode node, String sid) {
        nodeType = node.getNodeType();
        nodeLabel = node.getTitle();
        nodePath = node.getPath();
        treeName = node.getTreeConfigName();
        nodeName = node.getName();
        sessionId = sid;
    }

    @Override
    protected RemotePublicationTreeManager getPublisher() {
        if (service == null) {
            service = Framework.getService(RemotePublicationTreeManager.class);
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
    protected PublicationNode switchToServerNode(PublicationNode node) {
        // no wrap
        return node;
    }

    @Override
    protected PublicationNode switchToClientNode(PublicationNode node) {
        // no wrap
        return node;
    }

}
