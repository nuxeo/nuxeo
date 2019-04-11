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
     * Returns the internationalized label for a given path of the specified Directory tree. The Directory tree label is
     * not included.
     *
     * @since 5.4
     */
    String getLabelFor(String directoryTreeName, String fullPath);

    /**
     * Returns the internationalized label for a given path of the specified Directory tree, including or not the
     * Directory tree label.
     *
     * @since 5.4
     */
    String getLabelFor(String directoryTreeName, String fullPath, boolean includeDirectoryTreeLabel);

}
