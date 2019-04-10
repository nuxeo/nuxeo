/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.io.fsexporter;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Plugin for FS Exporter that exports documents like they should appear in Nuxeo Drive.
 *
 * @since 10.3
 */
public class DriveLikeExporterPlugin extends DefaultExporterPlugin {

    @Override
    public File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws IOException {
        File folder = null;
        File newFolder = null;
        folder = new File(fsPath);

        // if target directory doesn't exist, create it
        if (!folder.exists()) {
            folder.mkdir();
        }

        if ("/".equals(docfrom.getPathAsString())) {
            // we do not serialize the root document
            return folder;
        }

        if (docfrom.isFolder()) {
            String fileName = StringUtils.isNotBlank(docfrom.getTitle()) ? docfrom.getTitle() : docfrom.getName();
            newFolder = avoidingCollision(new File(fsPath + "/" + fileName));

            newFolder.mkdir();
        }

        // get all the blobs of the blob holder
        BlobHolder myblobholder = docfrom.getAdapter(BlobHolder.class);
        if (myblobholder != null && myblobholder.getBlob() != null) {

            Blob blob = myblobholder.getBlob();
            String filename = blob.getFilename();
            File target = avoidingCollision(new File(folder, filename));
            blob.transferTo(target);
        }

        if (newFolder != null) {
            folder = newFolder;
        }
        return folder;
    }

    /**
     * Given a file one wants to create, returns a file which name doesn't collide with the already existing files. For
     * files, the anti-collide index is added before the extension.
     *
     * @param file The file to create
     * @return a file that can be created.
     * @since 10.3
     */
    private File avoidingCollision(File file) {
        int i = 1;
        while (file.exists()) {
            // If there is an extension
            String name = file.getName();
            if (file.isFile() && name.indexOf(".") > 0) {
                String namePart = name.substring(0, name.indexOf("."));
                String extPart = name.substring(name.indexOf("."));

                file = new File(file.getParent(), namePart + "_" + i++ + extPart);
            } else {
                file = new File(file.getAbsolutePath() + "_" + i++);
            }
        }
        return file;
    }

}
