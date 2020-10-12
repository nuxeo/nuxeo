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
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

// Not used but please keep, it will be needed once the
// FileManagerService#createDefaultFolder method is extracted to a plugin.
public abstract class AbstractFolderImporter implements FolderImporter {

    protected String name = "";

    // to be used by plugin implementation to gain access to standard file
    // creation utility methods without having to lookup the service
    /**
     * @deprecated since 11.1, use {@link Framework#getService(Class)} instead if needed
     */
    @Deprecated(since = "11.1")
    protected FileManagerService fileManagerService;

    protected AbstractFolderImporter() {
        this.fileManagerService = (FileManagerService) Framework.getService(FileManager.class);
    }

    @Override
    public DocumentModel create(CoreSession documentManager, String fullname, String path, boolean overwrite,
            TypeManager typeManager) {
        // sample implementation to override in a custom FolderImporter
        // implementation
        return ((FileManagerService) Framework.getService(FileManager.class)).defaultCreateFolder(documentManager,
                fullname, path, overwrite);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setFileManagerService(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

}
