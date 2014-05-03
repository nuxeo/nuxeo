/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.tree;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.richfaces.component.UITree;
import org.richfaces.event.CollapsibleSubTableToggleEvent;
import org.richfaces.event.TreeSelectionChangeEvent;

/**
 * Local interface for the Seam component that manages the tree.
 *
 * @author Razvan Caraghin
 * @author Anahide Tchertchian
 */
public interface TreeActions {

    String DEFAULT_TREE_PLUGIN_NAME = "navigation";

    /**
     * Returns tree roots according to current document first accessible
     * parent.
     */
    List<DocumentTreeNode> getTreeRoots() throws ClientException;

    /**
     * Listener for node opening/closing events.
     * <p>
     * Used to not interfere with node state when manually changing open nodes.
     */
    @Deprecated
    void changeExpandListener(CollapsibleSubTableToggleEvent event);

    void toggleListener();

    /**
     * Returns true if node should be opened according to current document.
     */

    @Deprecated
    Boolean adviseNodeOpened(UITree tree);

    void resetCurrentDocumentData();

    void reset();

}
