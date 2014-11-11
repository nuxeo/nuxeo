/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.util.List;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Manages the tree for reduced utilization, like for popup. Performs additional
 * work when a node is selected such as saving the selection and redirecting
 * towards the required page.
 * <p>
 * The scope is PAGE to reinitialize the tree for new utilization.
 *
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
@Scope(PAGE)
@Name("reducedTreeActions")
@Install(precedence = FRAMEWORK)
public class ReducedTreeActionsBean extends TreeActionsBean {

    private static final long serialVersionUID = 1L;

    @Override
    public List<DocumentTreeNode> getTreeRoots() throws ClientException {
        return getTreeRoots(true);
    }

    /**
     * @since 5.4
     * @return a list containing a DocumentTreeNode for the Root document
     * @throws ClientException
     */
    public List<DocumentTreeNode> getRootNode() throws ClientException {
        return getTreeRoots(true, documentManager.getRootDocument());
    }

}
