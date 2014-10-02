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
 * $Id: DirectoryTreeManager.java 28950 2008-01-11 13:35:06Z tdelprat $
 */
package org.nuxeo.ecm.webapp.directory;

import java.io.Serializable;
import java.util.List;

public interface DirectoryTreeManager extends Serializable {

    DirectoryTreeNode get(String treeName);

    DirectoryTreeNode getSelectedTree();

    List<DirectoryTreeNode> getDirectoryTrees();

    List<String> getDirectoryTreeNames();

    String getSelectedTreeName();

    void setSelectedTreeName(String treeName);

    boolean isInitialized();

    /**
     * Returns the internationalized label for a given path of the specified
     * Directory tree. The Directory tree label is not included.
     *
     * @since 5.4
     */
    public String getLabelFor(String directoryTreeName, String fullPath);

    /**
     * Returns the internationalized label for a given path of the specified
     * Directory tree, including or not the Directory tree label.
     *
     * @since 5.4
     */
    public String getLabelFor(String directoryTreeName, String fullPath,
            boolean includeDirectoryTreeLabel);

}
