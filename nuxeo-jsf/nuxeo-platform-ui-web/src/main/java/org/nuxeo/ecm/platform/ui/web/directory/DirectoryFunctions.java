/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
