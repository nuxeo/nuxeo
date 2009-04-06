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
 * $Id: OOoMimetypeSniffer.java 20548 2007-06-15 13:08:36Z ogrisel $
 */

package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sf.jmimemagic.MagicDetector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;


public class OOoMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(OOoMimetypeSniffer.class);

    public String getDisplayName() {
        return "OOo 1.x & OpenDocument MimeType Detector";
    }

    public String[] getHandledExtensions() {
        return new String[] { "ods", "ots", "odt", "ott", "odp", "otp", "odg",
                "otg", "otm", "oth", "odi", "oti", "odf", "otf", "odc", "otc",
                "sxw", "stw", "sxg", "sxc", "stc", "sxi", "sti", "sxd", "std",
                "sxm", };
    }

    public String[] getHandledTypes() {
        return new String[] {
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.oasis.opendocument.spreadsheet-template",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.text-template",
                "application/vnd.oasis.opendocument.presentation",
                "application/vnd.oasis.opendocument.presentation-template",
                "application/vnd.oasis.opendocument.graphics",
                "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.text-master",
                "application/vnd.oasis.opendocument.text-web",
                "application/vnd.oasis.opendocument.image",
                "application/vnd.oasis.opendocument.image-template",
                "application/vnd.oasis.opendocument.formula",
                "application/vnd.oasis.opendocument.formula-template",
                "application/vnd.oasis.opendocument.chart",
                "application/vnd.oasis.opendocument.chart-template",
                // OOo 1.x file format
                "application/vnd.sun.xml.writer",
                "application/vnd.sun.xml.writer.template",
                "application/vnd.sun.xml.writer.global",
                "application/vnd.sun.xml.calc",
                "application/vnd.sun.xml.calc.template",
                "application/vnd.sun.xml.impress",
                "application/vnd.sun.xml.impress.template",
                "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template",
                "application/vnd.sun.xml.math", };
    }

    public String getName() {
        return "ooodetector";
    }

    public String getVersion() {
        return "0.2";
    }

    public String[] process(byte[] data, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {
        String[] mimetypes = {};
        File file = null;
        try {
            file = File.createTempFile("magicdetector", ".xml");
            FileUtils.writeFile(file, data);
            mimetypes = guessOOo(file);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
        return mimetypes;
    }

    public String[] process(File file, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {
        return guessOOo(file);
    }

    public String[] guessOOo(File file) {

        String[] mimetype = {};
        File tempFile = null;

        try {
            ZipFile zip = new ZipFile(file);
            ZipEntry entry = zip.getEntry("mimetype");

            if (entry != null) {
                // we have an opendocument so lets unzip

                //unzip file to process xml content
                tempFile = File.createTempFile("nxMimeTypeDetector_", ".dir");
                tempFile.delete(); // to be able to create a dir under this name
                if (!tempFile.isDirectory()) {
                    tempFile.mkdir();
                }
                ZipUtils.unzip(file, tempFile);

                // retrieves mimetypefile
                String path = tempFile.getAbsolutePath();
                path += File.separator + "mimetype";
                File mimetypeFile = new File(path);
                mimetype = new String[] { FileUtils.readFile(mimetypeFile) };
            }
        } catch (IOException e) {
            // probably not a zip file
        } finally {
            if (tempFile != null) {
                FileUtils.deleteTree(tempFile);
            }
        }

        return mimetype;
    }

}
