/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.doc.bonita;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.runtime.api.Framework;

/**
 * Exports operations and automation chains for Bonita
 * <ul>
 * Produces a Zip with:
 * <li>the Java class and associated XML file for a given operation or
 * automation chain</li>
 * <li>all the Java classes and associated XML files for all operations and
 * automation chains (expect operations defined for UI that are not useful for
 * Bonita)</li>
 * </ul>
 * <p>
 * As every Nuxeo operation needs an authentication on the server, login,
 * password and url information are always added, with specific names to avoid
 * collisions with operation parameters.
 * <p>
 * As Bonita does not do the distinction between operation parameters and
 * operation input (everything's an input), all kinds of Nuxeo inputs are also
 * alaways added, with specific names to avoid collisions with operation
 * parameters.
 *
 * @since 5.4.1
 */
public class BonitaExporter {

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    public static enum ZIP_ENTRY_ENCODING_OPTIONS {
        ascii
    }

    private static final int BUFFER = 2048;

    public static String getJavaClass(OperationDocumentation doc)
            throws IOException {
        BonitaOperationDocumentation op = new BonitaOperationDocumentation(doc);
        BonitaJavaExporter exporter = new BonitaJavaExporter(op);
        return exporter.run();
    }

    public static String getXMLDescription(OperationDocumentation doc)
            throws IOException, UnsupportedEncodingException {
        BonitaOperationDocumentation op = new BonitaOperationDocumentation(doc);
        BonitaXMLExporter exporter = new BonitaXMLExporter(op);
        return exporter.run();
    }

    public static InputStream toZip() throws Exception {
        return toZip(Framework.getService(AutomationService.class).getDocumentation());
    }

    public static InputStream toZip(List<OperationDocumentation> docs)
            throws IOException {
        File tmpFile = File.createTempFile("NX-ZipExport-", ".zip");
        tmpFile.deleteOnExit();

        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream zipout = new ZipOutputStream(fout);
        zipout.setMethod(ZipOutputStream.DEFLATED);
        zipout.setLevel(9);
        byte[] data = new byte[BUFFER];

        for (OperationDocumentation doc : docs) {
            addToZip(zipout, data, doc.getId() + ".java", getJavaClass(doc));
            addToZip(zipout, data, doc.getId() + ".xml", getXMLDescription(doc));
        }

        try {
            zipout.close();
            fout.close();
        } catch (ZipException e) {
            return null;
        }

        return new FileInputStream(tmpFile);

    }

    public static InputStream getZip(OperationDocumentation doc)
            throws IOException {
        File tmpFile = File.createTempFile("NX-ZipExport-", ".zip");
        tmpFile.deleteOnExit();

        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream zipout = new ZipOutputStream(fout);
        zipout.setMethod(ZipOutputStream.DEFLATED);
        zipout.setLevel(9);
        byte[] data = new byte[BUFFER];

        addToZip(zipout, data, doc.getId() + ".java", getJavaClass(doc));
        addToZip(zipout, data, doc.getId() + ".xml", getXMLDescription(doc));

        try {
            zipout.close();
            fout.close();
        } catch (ZipException e) {
            return null;
        }

        return new FileInputStream(tmpFile);
    }

    protected static void addToZip(ZipOutputStream zip, byte[] data,
            String path, String content) throws IOException {
        BufferedInputStream buffi = new BufferedInputStream(
                new ByteArrayInputStream(
                        content.getBytes(BonitaExportConstants.ENCODING)),
                BUFFER);
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        int count = buffi.read(data, 0, BUFFER);

        while (count != -1) {
            zip.write(data, 0, count);
            count = buffi.read(data, 0, BUFFER);
        }
        zip.closeEntry();
        buffi.close();
    }

}
