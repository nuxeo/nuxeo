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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.api.*;
import org.nuxeo.runtime.api.Framework;

import java.util.List;
import java.util.Map;

/**
 * Remotable implementation of the {@link PublicationTree} interface. Because some {@link PublicationTree}
 * implementation may be bound to local resources (network connexions, local filesystem ...) all {@link PublicationTree}
 * returned by the service are wrapped into this RemotablePublicationTree.
 *
 * @author tiry
 */
public class ProxyTree extends AbstractRemotableTree implements PublicationTree {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String title;

    protected String treeType;

    protected String path;

    protected String iconCollapsed;

    protected String iconExpanded;

    protected ProxyNode rootNode;

    protected String treeTitle;

    protected List<PublicationNode> childrenNodes;

    protected String getTargetTreeName() {
        return name;
    }

    @Override
    protected List<PublicationNode> switchToClientNodes(List<PublicationNode> nodes) {
        return nodes;
    }

    protected RemotePublicationTreeManager getTreeService() {
        if (treeService == null) {
            treeService = Framework.getService(RemotePublicationTreeManager.class);
        }
        return treeService;
    }

    public ProxyTree(PublicationTree tree, String sid) {
        this.sessionId = sid;
        this.name = tree.getName();
        this.title = tree.getTitle();
        this.treeTitle = tree.getTreeTitle();
        this.treeType = tree.getTreeType();
        this.path = tree.getPath();
        this.rootNode = new ProxyNode(tree, sid);
        this.configName = tree.getConfigName();
        this.iconCollapsed = tree.getIconCollapsed();
        this.iconExpanded = tree.getIconExpanded();
    }

    public String getTitle() {
        return title;
    }

    public String getTreeTitle() {
        return treeTitle;
    }

    public String getName() {
        return name;
    }

    public String getTreeType() {
        return treeType;
    }

    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName) {
        // NOP
    }

    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName, String title) {
        // NOP
    }

    public List<PublishedDocument> getChildrenDocuments() {
        return rootNode.getChildrenDocuments();
    }

    public String getNodeType() {
        return rootNode.getNodeType();
    }

    public String getPath() {
        return path;
    }

    public String getTreeConfigName() {
        return configName;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    protected String getServerTreeSessionId() {
        return getSessionId();
    }

    // make fail tests with surefire !
    /*
     * @Override protected void finalize() throws Throwable { try { release(); } finally { super.finalize(); } }
     */

    public List<PublicationNode> getChildrenNodes() {
        if (childrenNodes == null) {
            childrenNodes = rootNode.getChildrenNodes();
        }
        return childrenNodes;
    }

    /**
     * @since 8.10-HF06
     */
    public void setChildrenNodes(List<PublicationNode> childrenNodes) {
        rootNode.setChildrenNodes(childrenNodes);
        this.childrenNodes = childrenNodes;
    }

    @Override
    protected PublicationNode switchToClientNode(PublicationNode node) {
        // no wrap
        return node;
    }

    @Override
    protected PublicationNode switchToServerNode(PublicationNode node) {
        return node;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getIconExpanded() {
        return iconExpanded;
    }

    public String getIconCollapsed() {
        return iconCollapsed;
    }

}
