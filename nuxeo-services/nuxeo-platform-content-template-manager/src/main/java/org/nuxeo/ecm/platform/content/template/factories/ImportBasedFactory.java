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

package org.nuxeo.ecm.platform.content.template.factories;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.content.template.service.ACEDescriptor;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This factory will import a file using a path defined in the option of the factoryBinding extension point. This path
 * can be defined using three different prefix. absolute:myAbsolute path will reference a file on the server's
 * filesystem, nxData:myPath will reference a file inside nuxeo data folder and resource:myPath will reference a file in
 * the bundle's resources. If the file exist, it's imported by the {@link FileManager} service.
 *
 * @author ldoguin
 */
public class ImportBasedFactory extends BaseContentFactory {

    private static final Log log = LogFactory.getLog(ImportBasedFactory.class);

    public static final String IMPORT_FILE_PATH_OPTION = "path";

    public static final String IMPORT_OVERWRITE_OPTION = "overwrite";

    protected FileManager fileManager;

    protected Map<String, String> options;

    protected File importedFile;

    protected Boolean overwrite = false;

    public enum PathOptions {
        resource {
            @Override
            public File getFile(String path) {
                return FileUtils.getResourceFileFromContext(path);
            }
        },
        nxData {
            @Override
            public File getFile(String path) {
                File nxDdataFolder = Environment.getDefault().getData();
                return new File(nxDdataFolder, path);
            }
        },
        absolute {
            @Override
            public File getFile(String path) {
                return new File(path);
            }
        };

        protected abstract File getFile(String path);

        public static File getResource(String path) {
            String[] s = path.split(":", 2);
            String resourceType = s[0];
            String resourcePath = s[1];
            PathOptions option = valueOf(resourceType);
            if (option == null) {
                log.error("Unsupported resource type: " + resourceType);
                return null;
            } else {
                return option.getFile(resourcePath);
            }
        }
    }

    @Override
    public boolean initFactory(Map<String, String> options, List<ACEDescriptor> rootAcl,
            List<TemplateItemDescriptor> template) {
        this.options = options;
        overwrite = Boolean.valueOf(options.get(IMPORT_OVERWRITE_OPTION));
        String path = options.get(IMPORT_FILE_PATH_OPTION);
        File file = PathOptions.getResource(path);
        if (file != null) {
            if (file.exists()) {
                importedFile = file;
                return true;
            } else {
                log.warn("Following file does not exist: " + file.getAbsolutePath());
            }
        }
        return false;
    }

    @Override
    public void createContentStructure(DocumentModel eventDoc) throws ClientException {
        initSession(eventDoc);

        if (eventDoc.isVersion()) {
            return;
        }
        try {
            String parentPath = eventDoc.getPathAsString();
            importBlob(importedFile, parentPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Use fileManager to import the given file.
     *
     * @param file to import
     * @param parentPath of the targetDocument
     * @throws Exception
     */
    protected void importBlob(File file, String parentPath) throws Exception {
        if (file.isDirectory()) {
            DocumentModel createdFolder = getFileManagerService().createFolder(
                    session, file.getAbsolutePath(), parentPath);
            File[] files = file.listFiles();
            for (File childFile : files) {
                importBlob(childFile, createdFolder.getPathAsString());
            }
        } else {
            Blob fb = new FileBlob(file);
            fb.setFilename(file.getName());
            getFileManagerService().createDocumentFromBlob(session, fb, parentPath, overwrite, fb.getFilename());
        }
    }

    protected FileManager getFileManagerService() {
        if (fileManager == null) {
            try {
                fileManager = Framework.getService(FileManager.class);
            } catch (Exception e) {
                throw new RuntimeException("Unable to get FileManager service ", e);
            }
        }
        return fileManager;
    }
}
