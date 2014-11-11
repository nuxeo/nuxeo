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
