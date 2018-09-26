/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * Simple Plugin that imports IO Zip archive into Nuxeo using the IO core service.
 *
 * @author tiry
 */
public class ExportedZipImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1876876876L;

    private static final Log log = LogFactory.getLog(ExportedZipImporter.class);

    public static ZipFile getArchiveFileIfValid(File file) throws IOException {
        ZipFile zip;

        try {
            zip = new ZipFile(file);
        } catch (ZipException e) {
            log.debug("file is not a zipfile ! ", e);
            return null;
        } catch (IOException e) {
            log.debug("can not open zipfile ! ", e);
            return null;
        }

        ZipEntry marker = zip.getEntry(".nuxeo-archive");

        if (marker == null) {
            zip.close();
            return null;
        } else {
            return zip;
        }
    }

    @Override
    public boolean isOneToMany() {
        return true;
    }

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite,
            String filename, TypeManager typeService) throws IOException {
        try (CloseableFile source = content.getCloseableFile()) {
            ZipFile zip = getArchiveFileIfValid(source.getFile());
            if (zip == null) {
                return null;
            }
            zip.close();

            boolean importWithIds = false;
            DocumentReader reader = new NuxeoArchiveReader(source.getFile());
            ExportedDocument root = reader.read();
            IdRef rootRef = new IdRef(root.getId());

            if (documentManager.exists(rootRef)) {
                DocumentModel target = documentManager.getDocument(rootRef);
                if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                    importWithIds = true;
                }
            }

            DocumentWriter writer = new DocumentModelWriter(documentManager, path, 10);
            reader.close();
            reader = new NuxeoArchiveReader(source.getFile());

            DocumentRef resultingRef;
            if (overwrite && importWithIds) {
                resultingRef = rootRef;
            } else {
                String rootName = root.getPath().lastSegment();
                resultingRef = new PathRef(path, rootName);
            }

            try {
                DocumentPipe pipe = new DocumentPipeImpl(10);
                pipe.setReader(reader);
                pipe.setWriter(writer);
                pipe.run();
            } catch (IOException e) {
                log.warn(e, e);
            } finally {
                reader.close();
                writer.close();
            }
            return documentManager.getDocument(resultingRef);
        }
    }
}
