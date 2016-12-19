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
package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.api.Framework;

import net.sf.jmimemagic.MagicDetector;

public class OOoMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(OOoMimetypeSniffer.class);

    @Override
    public String getDisplayName() {
        return "OOo 1.x & OpenDocument MimeType Detector";
    }

    @Override
    public String[] getHandledExtensions() {
        return new String[] { "ods", "ots", "odt", "ott", "odp", "otp", "odg", "otg", "otm", "oth", "odi", "oti", "odf",
                "otf", "odc", "otc", "sxw", "stw", "sxg", "sxc", "stc", "sxi", "sti", "sxd", "std", "sxm", };
    }

    @Override
    public String[] getHandledTypes() {
        return new String[] { "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.oasis.opendocument.spreadsheet-template", "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.text-template", "application/vnd.oasis.opendocument.presentation",
                "application/vnd.oasis.opendocument.presentation-template",
                "application/vnd.oasis.opendocument.graphics", "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.text-master", "application/vnd.oasis.opendocument.text-web",
                "application/vnd.oasis.opendocument.image", "application/vnd.oasis.opendocument.image-template",
                "application/vnd.oasis.opendocument.formula", "application/vnd.oasis.opendocument.formula-template",
                "application/vnd.oasis.opendocument.chart", "application/vnd.oasis.opendocument.chart-template",
                // OOo 1.x file format
                "application/vnd.sun.xml.writer", "application/vnd.sun.xml.writer.template",
                "application/vnd.sun.xml.writer.global", "application/vnd.sun.xml.calc",
                "application/vnd.sun.xml.calc.template", "application/vnd.sun.xml.impress",
                "application/vnd.sun.xml.impress.template", "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template", "application/vnd.sun.xml.math", };
    }

    @Override
    public String getName() {
        return "ooodetector";
    }

    @Override
    public String getVersion() {
        return "0.2";
    }

    @Override
    public String[] process(byte[] data, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {
        String[] mimetypes = {};
        File file = null;
        try {
            file = Framework.createTempFile("magicdetector", ".xml");
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

    @Override
    public String[] process(File file, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {
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

                // unzip file to process xml content
                tempFile = Framework.createTempFile("nxMimeTypeDetector_", ".dir");
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
                org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            }
        }

        return mimetype;
    }

}
