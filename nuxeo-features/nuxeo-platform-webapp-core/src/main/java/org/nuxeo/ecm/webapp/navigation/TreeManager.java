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
package org.nuxeo.ecm.webapp.navigation;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tree.LazyTreeModel;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Local interface for the Seam component that manages the tree.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Local
@Remote
public interface TreeManager extends StatefulBaseLifeCycle {

    void initialize() throws ClientException;

    /**
     * Reverts the tree to uninitialized state.
     */
    void reset();

    boolean isInitialized() throws ClientException;

    String selectNode() throws ClientException;

    @Remove
    @Destroy
    @PermitAll
    void destroy();

    LazyTreeModel getTreeModel() throws ClientException;

    void invalidateSyncedState() throws ClientException;

    void expandToCurrentTreeNode() throws ClientException;

    void refreshTreeNodeChildren() throws ClientException;

    void refreshTreeNodeDescription() throws ClientException;

    /**
     * Listener for the events that must trigger the reset
     * of the children branch of under the given document.
     *
     * @param targetDoc
     * @throws ClientException
     */
    void refreshTreeNodeChildren(DocumentModel targetDoc) throws ClientException;
}
