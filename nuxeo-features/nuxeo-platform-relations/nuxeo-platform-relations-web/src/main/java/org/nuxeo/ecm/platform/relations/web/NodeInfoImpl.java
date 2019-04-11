/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NodeInfoImpl.java 21693 2007-07-01 08:00:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class NodeInfoImpl implements NodeInfo {

    private static final long serialVersionUID = -5807130430819964154L;

    protected final Node node;

    protected DocumentModel documentModel;

    protected boolean visible = false;

    public NodeInfoImpl(Node node) {
        this.node = node;
    }

    public NodeInfoImpl(Node node, DocumentModel documentModel) {
        this.node = node;
        this.documentModel = documentModel;
    }

    public NodeInfoImpl(Node node, DocumentModel documentModel, boolean visible) {
        this.node = node;
        this.documentModel = documentModel;
        this.visible = visible;
    }

    // Node interface

    @Override
    public NodeType getNodeType() {
        return node.getNodeType();
    }

    @Override
    public boolean isBlank() {
        return node.isBlank();
    }

    @Override
    public boolean isLiteral() {
        return node.isLiteral();
    }

    @Override
    public boolean isQNameResource() {
        return node.isQNameResource();
    }

    @Override
    public boolean isResource() {
        return node.isResource();
    }

    // NodeInfo interface

    @Override
    public int compareTo(Node o) {
        return node.compareTo(o);
    }

    // Node Representation interface

    @Override
    public String getAction() {
        if (visible && documentModel != null) {
            String docId = documentModel.getId();
            String actionValue = String.format("#{navigationContext.navigateToId('%s')}", docId);
            return actionValue;
        }
        return null;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    @Override
    public String getHref() {
        if (documentModel == null && isResource()) {
            return ((Resource) node).getUri();
        }
        return null;
    }

    @Override
    public String getIcon() {
        return DocumentModelFunctions.iconPath(documentModel);
    }

    @Override
    public String getTitle() {
        String title = null;
        if (node.isLiteral()) {
            title = ((Literal) node).getValue();
        } else if (node.isQNameResource()) {
            String resourceTitle = null;
            QNameResource resource = (QNameResource) node;
            if (documentModel != null) {
                String documentTitle = (String) documentModel.getProperty("dublincore", "title");
                if (documentTitle != null && documentTitle.length() > 0) {
                    resourceTitle = documentTitle;
                }
            }
            if (resourceTitle == null) {
                title = resource.getUri();
            } else {
                title = resourceTitle;
            }
        } else if (node.isResource()) {
            title = ((Resource) node).getUri();
        }
        return title;
    }

    @Override
    public boolean isLink() {
        return getHref() != null;
    }

    @Override
    public boolean isDocument() {
        return documentModel != null;
    }

    @Override
    public boolean isDocumentVisible() {
        return documentModel != null && visible;
    }

    @Override
    public boolean isText() {
        return !(isDocumentVisible() || isLink());
    }

}
