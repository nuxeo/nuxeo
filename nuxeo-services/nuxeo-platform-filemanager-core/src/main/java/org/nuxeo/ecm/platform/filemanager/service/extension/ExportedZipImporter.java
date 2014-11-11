/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
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
import org.nuxeo.ecm.core.api.ClientException;
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
 * Simple Plugin that imports IO Zip achive into Nuxeo using the IO core service.
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

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws ClientException, IOException {

        File tmp = File.createTempFile("xml-importer", null);

        content.transferTo(tmp);

        ZipFile zip = getArchiveFileIfValid(tmp);

        if (zip == null) {
            tmp.delete();
            return null;
        }

        boolean importWithIds = false;
        DocumentReader reader = new NuxeoArchiveReader(tmp);
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
        reader = new NuxeoArchiveReader(tmp);

        DocumentRef resultingRef;
        if (overwrite && importWithIds) {
            resultingRef = rootRef;
        } else {
            String rootName = root.getPath().lastSegment();
            resultingRef = new PathRef(path + "/" + rootName);
        }

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } catch (Exception e) {
        } finally {
            reader.close();
            writer.close();
        }
        tmp.delete();
        if (resultingRef != null) {
            return documentManager.getDocument(resultingRef);
        }
        return null;
    }
}
