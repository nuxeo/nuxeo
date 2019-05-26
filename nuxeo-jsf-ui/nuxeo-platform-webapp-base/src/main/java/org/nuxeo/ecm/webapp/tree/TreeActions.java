/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.ecm.webapp.tree;

import java.util.List;


/**
 * Local interface for the Seam component that manages the tree.
 *
 * @author Razvan Caraghin
 * @author Anahide Tchertchian
 */
public interface TreeActions {

    String DEFAULT_TREE_PLUGIN_NAME = "navigation";

    /**
     * Returns tree roots according to current document first accessible parent.
     */
    List<DocumentTreeNode> getTreeRoots();

    String getCurrentDocumentPath();

    void resetCurrentDocumentData();

    void reset();

    /**
     * @since 6.0
     */
    void toggleListener();

    /**
     * @since 6.0
     */
    boolean isNodeExpandEvent();

}
