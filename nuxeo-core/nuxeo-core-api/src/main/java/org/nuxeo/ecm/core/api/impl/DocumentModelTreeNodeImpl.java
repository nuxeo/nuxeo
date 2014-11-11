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
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;

/**
 * Simplistic representation of a node in a tree that is actually a list
 * that has levels.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Paslaru Narcis</a>
 */
public class DocumentModelTreeNodeImpl implements Serializable, DocumentModelTreeNode{

    private static final long serialVersionUID = -1549177060872366505L;

    // XXX NP : to be removed along with getTreeLabel
    @Deprecated
    private static final String HTML_TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

    private DocumentModel document;
    private Integer levelTree;
    private String version;

    public DocumentModelTreeNodeImpl(DocumentModel doc, int level) {
        document = doc;
        levelTree = level;
    }

    // TODO NP : - to be removed asap
    @Deprecated
    public String getTreeLabel() {
        StringBuilder label = new StringBuilder("");
        for (int i = 0; i < levelTree; i++) {
            label.append(HTML_TAB);
        }
        label.append(document.getProperty("dublincore", "title"));
        return label.toString();
    }

    public DocumentModel getDocument() {
        return document;
    }

    // XXX: remove ?
    public void setDocument(DocumentModel document) {
        this.document = document;
    }

    public Integer getLevelTree() {
        return levelTree;
    }

    // XXX: remove ?
    public void setLevelTree(Integer levelTree) {
        this.levelTree = levelTree;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // What if levelTree or document == null ?
        if (obj instanceof DocumentModelTreeNodeImpl) {
            DocumentModelTreeNodeImpl altDoc = (DocumentModelTreeNodeImpl) obj;
            return altDoc.levelTree.equals(levelTree)
                    && altDoc.document.equals(document);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = document.hashCode();
        result = 31 * result + levelTree.hashCode();
        result = 31 * result + (version == null ? 0 : version.hashCode());
        return result;
    }

}
