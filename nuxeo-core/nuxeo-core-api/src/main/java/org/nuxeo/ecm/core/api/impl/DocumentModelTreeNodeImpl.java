/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private DocumentModel document;
    private Integer levelTree;
    private String version;

    public DocumentModelTreeNodeImpl(DocumentModel doc, int level) {
        document = doc;
        levelTree = level;
    }

    @Override
    public DocumentModel getDocument() {
        return document;
    }

    // TODO: remove
    public void setDocument(DocumentModel document) {
        this.document = document;
    }

    public Integer getLevelTree() {
        return levelTree;
    }

    // TODO: remove
    public void setLevelTree(Integer levelTree) {
        this.levelTree = levelTree;
    }

    public String getVersion() {
        return version;
    }

    // TODO: remove
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
