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
 * $Id: Functions.java 19475 2007-05-27 10:33:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility functions (directory related) to be used from jsf via nxu: tags.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 * @author Anahide Tchertchian
 */
public final class DirectoryFunctions {

    /**
     * Utility classes should not have a public or default constructor.
     */
    private DirectoryFunctions() {
    }

    public static DocumentModel getDirectoryEntry(String directoryName, String entryId) {
        if (entryId == null) {
            return null;
        }
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        try (Session session = dirService.open(directoryName)) {
            return session.getEntry(entryId);
        }
    }

    public static DocumentModelList getDirectoryEntries(String directoryName, String... entryIds) {
        if (entryIds == null) {
            return null;
        }
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        try (Session session = dirService.open(directoryName)) {
            DocumentModelList result = new DocumentModelListImpl();
            for (String entryId : entryIds) {
                DocumentModel entry = session.getEntry(entryId);
                if (entry != null) {
                    result.add(entry);
                }
            }
            return result;
        }
    }

    public static DocumentModelList getDirectoryListEntries(String directoryName, Collection<String> entryIds) {
        if (entryIds == null) {
            return null;
        }
        return getDirectoryEntries(directoryName, entryIds.toArray(new String[] {}));
    }

}
