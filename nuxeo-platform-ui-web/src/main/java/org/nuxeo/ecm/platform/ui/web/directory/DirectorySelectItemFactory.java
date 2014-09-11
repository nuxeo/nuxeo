/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemFactory;

/**
 * @since 5.9.6
 */
public abstract class DirectorySelectItemFactory extends SelectItemFactory {

    private static final Log log = LogFactory.getLog(DirectorySelectItemFactory.class);

    protected abstract String getDirectoryName();

    protected Session getDirectorySession() {
        String dirName = getDirectoryName();
        return getDirectorySession(dirName);
    }

    protected static Session getDirectorySession(String dirName) {
        Session directorySession = null;
        if (dirName != null) {
            try {
                DirectoryService service = DirectoryHelper.getDirectoryService();
                directorySession = service.open(dirName);
            } catch (Exception e) {
                log.error(String.format("Error when retrieving directory %s",
                        dirName), e);
            }
        }
        return directorySession;
    }

    protected static void closeDirectorySession(Session directorySession) {
        if (directorySession != null) {
            try {
                directorySession.close();
            } catch (DirectoryException e) {
            }
        }
    }

    public SelectItem createSelectItem(Object value) {
        SelectItem item = null;

        if (value instanceof SelectItem) {
            item = (SelectItem) value;
        } else if (value instanceof String) {
            Object varValue = saveRequestMapVarValue();
            try {
                String entryId = (String) value;
                putIteratorToRequestParam(value);
                Session directorySession = getDirectorySession();
                if (directorySession != null) {
                    try {
                        DocumentModel entry = directorySession.getEntry(entryId);
                        if (entry != null) {
                            putIteratorToRequestParam(entry);
                            item = createSelectItem();
                            removeIteratorFromRequestParam();
                        }
                    } catch (DirectoryException e) {
                    }
                } else {
                    log.error("No session provided for directory, returning empty selection");
                }
                closeDirectorySession(directorySession);

                removeIteratorFromRequestParam();
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        }
        return item;
    }

}
