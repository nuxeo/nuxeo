/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NodeInfoImpl.java 21693 2007-07-01 08:00:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
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

    public NodeType getNodeType() {
        return node.getNodeType();
    }

    public boolean isBlank() {
        return node.isBlank();
    }

    public boolean isLiteral() {
        return node.isLiteral();
    }

    public boolean isQNameResource() {
        return node.isQNameResource();
    }

    public boolean isResource() {
        return node.isResource();
    }

    // NodeInfo interface

    public int compareTo(Node o) {
        return node.compareTo(o);
    }

    // Node Representation interface

    public String getAction() {
        if (visible && documentModel != null) {
            String docId = documentModel.getId();
            String actionValue = String.format(
                    "#{navigationContext.navigateToId('%s')}", docId);
            return actionValue;
        }
        return null;
    }

    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    public String getHref() {
        if (documentModel == null && isResource()) {
            return ((Resource) node).getUri();
        }
        return null;
    }

    public String getIcon() {
        return DocumentModelFunctions.iconPath(documentModel);
    }

    public String getTitle() {
        String title = null;
        if (node.isLiteral()) {
            title = ((Literal) node).getValue();
        } else if (node.isQNameResource()) {
            String resourceTitle = null;
            QNameResource resource = (QNameResource) node;
            if (documentModel != null) {
                String documentTitle;
                try {
                    documentTitle = (String) documentModel.getProperty(
                            "dublincore", "title");
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
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

    public boolean isLink() {
        return getHref() != null;
    }

    public boolean isDocument() {
        return documentModel != null;
    }

    public boolean isDocumentVisible() {
        return documentModel != null && visible;
    }

    public boolean isText() {
        return !(isDocumentVisible() || isLink());
    }

}
