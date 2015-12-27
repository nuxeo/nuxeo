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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.trashManagement;

/**
 * Seam component used to manage named lists of documents.
 * <p>
 * Managing the DM lists into this component instead of directly inside the Seam context offers the following
 * advantages:
 * <ul>
 * <li>DM Lists life cycle management can be done transparently, the DocumentsListsManager can use internal fields or
 * differently scoped variables (Conversation, Process ...)
 * <li>DocumentsListsManager provides (will) an Extension Point mechanism to register new names lists
 * <li>DocumentsListsManager provides add configurations to each lists
 * <ul>
 * <li>List Name
 * <li>List Icon
 * <li>List append behavior
 * <li>Category of the list
 * <li>...
 * </ul>
 * <li>DocumentsListsManager provides helpers features for merging and resetting lists
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
