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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to make Session behave as a source for a DirectoryCache instance
 *
 * @author Olivier Grisel
 */
public interface EntrySource {

    /**
     * @apiNote This method handles a {@link org.nuxeo.ecm.directory.api.DirectoryConstants#SYSTEM_ID_PROPERTY system
     *          id} when the directory has {@link org.nuxeo.ecm.directory.api.DirectoryConstants#EXTERNAL_ID_TYPE
     *          external-id} type
     */
    DocumentModel getEntryFromSource(String idOrSysId, boolean fetchReferences);

}
