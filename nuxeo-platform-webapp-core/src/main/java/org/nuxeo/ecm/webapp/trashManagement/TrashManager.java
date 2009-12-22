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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.trashManagement;

/**
 * Seam component used to manage named lists of documents.
 * <p>
 * Managing the DM lists into this component instead of directly inside the Seam
 * context offers the following advantages:
 * <ul>
 * <li> DM Lists life cycle management can be done transparently, the
 * DocumentsListsManager can use internal fields or differently scoped variables
 * (Conversation, Process ...)
 * <li> DocumentsListsManager provides (will) an Extension Point mechanism to
 * register new names lists
 * <li> DocumentsListsManager provides add configurations to each lists
 * <ul>
 * <li> List Name
 * <li> List Icon
 * <li> List append behavior
 * <li> Category of the list
 * <li> ...
 * </ul>
 * <li> DocumentsListsManager provides helpers features for merging and
 * resetting lists
 * </ul>
 *
 * @author tiry
 */
public interface TrashManager {

    /**
     * Checks if trash management is enabled.
     *
     * @return true if trash management is enabled
     */
    boolean isTrashManagementEnabled();

    void destroy();

    void initTrashManager();

}
