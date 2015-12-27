/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.util.List;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Manages the tree for reduced utilization, like for popup. Performs additional work when a node is selected such as
 * saving the selection and redirecting towards the required page.
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
    public List<DocumentTreeNode> getTreeRoots() {
        return getTreeRoots(true);
    }

    /**
     * @since 5.4
     * @return a list containing a DocumentTreeNode for the Root document
     */
    public List<DocumentTreeNode> getRootNode() {
        return getTreeRoots(true, documentManager.getRootDocument());
    }

}
