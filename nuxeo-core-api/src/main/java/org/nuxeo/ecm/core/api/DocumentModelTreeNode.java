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

package org.nuxeo.ecm.core.api;

/**
 * Simplistic representation of a node in a tree that is actually a list
 * that has levels.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Paslaru Narcis</a>
 */
public interface DocumentModelTreeNode {

    DocumentModel getDocument();

    /*
    @Deprecated
    void setDocument(DocumentModel document);

    @Deprecated
    Integer getLevelTree();

    @Deprecated
    void setLevelTree(Integer levelTree);

    @Deprecated
    void setVersion(String version);

    @Deprecated
    String getVersion();
    */
}
