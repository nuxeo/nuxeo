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

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.types.TypeManager;

// Not used but please keep, it will be needed once the
// FileManagerService#createDefaultFolder method is extracted to a plugin.
public abstract class AbstractFolderImporter implements FolderImporter {

    protected String name = "";

    // to be used by plugin implementation to gain access to standard file
    // creation utility methods without having to lookup the service
    protected FileManagerService fileManagerService;

    @Override
    public DocumentModel create(CoreSession documentManager, String fullname, String path, boolean overwrite,
            TypeManager typeManager) {
        // sample implementation to override in a custom FolderImporter
        // implementation
        return fileManagerService.defaultCreateFolder(documentManager, fullname, path);
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
