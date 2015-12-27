/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 6.0
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
            } catch (DirectoryException e) {
                log.error(String.format("Error when retrieving directory %s", dirName), e);
            }
        }
        return directorySession;
    }

    /**
     * @deprecated since 7.4. Directory sessions are now AutoCloseable.
     */
    @Deprecated
    protected static void closeDirectorySession(Session directorySession) {
        if (directorySession != null) {
            try {
                directorySession.close();
            } catch (DirectoryException e) {
            }
        }
    }

    @Override
    public SelectItem createSelectItem(Object value) {
        SelectItem item = null;
        if (value instanceof SelectItem) {
            Object varValue = saveRequestMapVarValue();
            try {
                putIteratorToRequestParam(value);
                item = createSelectItem();
                removeIteratorFromRequestParam();
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        } else if (value instanceof String) {
            Object varValue = saveRequestMapVarValue();
            try (Session directorySession = getDirectorySession()) {
                String entryId = (String) value;
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
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        }
        return item;
    }

}
