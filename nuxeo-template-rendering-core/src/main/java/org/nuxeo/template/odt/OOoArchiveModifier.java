/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.template.odt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Helper used to modify a ODT/Zip archive for addition Pictures (and potentially fragments)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class OOoArchiveModifier {

    public File updateArchive(File workingDir, File oooFile, List<Blob> blobs) throws IOException {
        if (blobs == null || blobs.size() == 0) {
            return oooFile;
        }

        File unzipDir = new File(workingDir, "unzip-" + oooFile.getName());
        unzipDir.mkdirs();

        ZipUtils.unzip(oooFile, unzipDir);

        File pictureDirs = new File(unzipDir, "Pictures");
        if (!pictureDirs.exists()) {
            pictureDirs.mkdir();
            pictureDirs = new File(unzipDir, "Pictures");
        }

        File contentDirs = new File(unzipDir, "Content");
        if (!contentDirs.exists()) {
            contentDirs.mkdir();
            contentDirs = new File(unzipDir, "Content");
        }

        StringBuilder blobsManifest = new StringBuilder();
        for (Blob blob : blobs) {
            if (blob.getMimeType().startsWith("image")) {
                FileUtils.copyToFile(blob.getStream(), new File(pictureDirs, blob.getFilename()));
            } else {
                FileUtils.copyToFile(blob.getStream(), new File(contentDirs, blob.getFilename()));
            }

            blobsManifest.append("<manifest:file-entry manifest:media-type=\"");
            blobsManifest.append(blob.getMimeType());
            if (blob.getMimeType().startsWith("image")) {
                blobsManifest.append("\" manifest:full-path=\"Pictures/");
            } else {
                blobsManifest.append("\" manifest:full-path=\"Content/");
            }
            blobsManifest.append(blob.getFilename());
            blobsManifest.append("\"/>\n");
        }

        File xmlManifestFile = new File(unzipDir.getPath() + "/META-INF/manifest.xml");
        String xmlManifest = FileUtils.readFile(xmlManifestFile);
        int idx = xmlManifest.indexOf("</manifest:manifest>");
        xmlManifest = xmlManifest.substring(0, idx) + blobsManifest.toString() + xmlManifest.substring(idx);
        FileUtils.writeFile(xmlManifestFile, xmlManifest.getBytes());

        String path = oooFile.getAbsolutePath();

        oooFile.delete();

        oooFile = new File(path);
        oooFile.createNewFile();

        // ZipUtils.zip(unzipDir.listFiles(), oooFile);
        mkOOoZip(unzipDir, oooFile);

        FileUtils.deleteTree(unzipDir);

        return oooFile;

    }

    protected void mkOOoZip(File directory, File outFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outFile))) {
            File manif = new File(directory, "mimetype");
            writeOOoEntry(zipOutputStream, manif.getName(), manif, ZipEntry.STORED);

            for (File fileEntry : directory.listFiles()) {
                if (!fileEntry.getName().equals(manif.getName())) {
                    writeOOoEntry(zipOutputStream, fileEntry.getName(), fileEntry, ZipEntry.DEFLATED);
                }
            }
        }
    }

    protected void writeOOoEntry(ZipOutputStream zipOutputStream, String entryName, File fileEntry, int zipMethod)
            throws IOException {

        if (fileEntry.isDirectory()) {
            entryName = entryName + "/";
            ZipEntry zentry = new ZipEntry(entryName);
            zipOutputStream.putNextEntry(zentry);
            zipOutputStream.closeEntry();
            for (File child : fileEntry.listFiles()) {
                writeOOoEntry(zipOutputStream, entryName + child.getName(), child, zipMethod);
            }
            return;
        }

        ZipEntry zipEntry = new ZipEntry(entryName);
        InputStream entryInputStream = new FileInputStream(fileEntry);
        zipEntry.setMethod(zipMethod);
        if (zipMethod == ZipEntry.STORED) {
            byte[] inputBytes = FileUtils.readBytes(entryInputStream);
            CRC32 crc = new CRC32();
            crc.update(inputBytes);
            zipEntry.setCrc(crc.getValue());
            zipEntry.setSize(inputBytes.length);
            zipEntry.setCompressedSize(inputBytes.length);
            zipOutputStream.putNextEntry(zipEntry);
            FileUtils.copy(new ByteArrayInputStream(inputBytes), zipOutputStream);
        } else {
            zipOutputStream.putNextEntry(zipEntry);
            FileUtils.copy(entryInputStream, zipOutputStream);
        }
        try {
            entryInputStream.close();
        } catch (IOException e) {
            // NOP
        }
        zipOutputStream.closeEntry();
    }

}
