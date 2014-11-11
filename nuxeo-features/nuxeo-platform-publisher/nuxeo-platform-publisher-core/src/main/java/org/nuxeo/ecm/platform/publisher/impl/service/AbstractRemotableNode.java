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
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for {@link PublicationNode} that delegate method calls to a
 * remote service.
 *
 * @author tiry
 */
public abstract class AbstractRemotableNode implements PublicationNode {

    protected abstract RemotePublicationTreeManager getPublisher()
            throws ClientException;

    protected abstract String getServerTreeSessionId();

    protected String treeName;

    protected String sessionId;

    protected RemotePublicationTreeManager service;

    protected abstract String getTargetTreeName();

    /**
     * switch node definition from client to server (for remote publishing)
     */
    protected abstract PublicationNode switchToServerNode(PublicationNode node)
            throws ClientException;

    /**
     * switch node definition from server to client (for remote publishing)
     */
    protected abstract PublicationNode switchToClientNode(PublicationNode node)
            throws ClientException;

    protected List<PublicationNode> switchToServerNodes(
            List<PublicationNode> nodes) throws ClientException {
        List<PublicationNode> wrappedNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            wrappedNodes.add(switchToServerNode(node));
        }
        return wrappedNodes;
    }

    protected List<PublicationNode> switchToClientNodes(
            List<PublicationNode> nodes) throws ClientException {
        List<PublicationNode> wrappedNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            wrappedNodes.add(switchToClientNode(node));
        }
        return wrappedNodes;
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        // return getService().getChildrenDocuments(getServerTreeSessionId(),
        // this);
        return getPublisher().getChildrenDocuments(switchToServerNode(this));
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        return switchToClientNodes((getPublisher().getChildrenNodes(switchToServerNode(this))));
    }

    public PublicationNode getParent() {
        try {
            return switchToClientNode(getPublisher().getParent(
                    switchToServerNode(this)));
        } catch (ClientException e) {
            return null;
        }
    }

}
