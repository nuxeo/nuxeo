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
 *
 */

package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipFile;

import net.sf.jmimemagic.MagicDetector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;

public class MsoXML2007MimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(MsoXML2007MimetypeSniffer.class);

    @Override
    public String getDisplayName() {
        return "Microsoft 2007 files MimeType Detector";
    }

    @Override
    public String[] getHandledExtensions() {
        return new String[] { "docm", "docx", "dotm", "dotx", "ppsm", "ppsx", "pptm", "pptx", "xlsb", "xlsm", "xlsx",
                "xps" };
    }

    @Override
    public String[] getHandledTypes() {
        return new String[] { "application/vnd.ms-word.document.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-word.template.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                "application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
                "application/vnd.ms-excel.sheet.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-xpsdocument", };
    }

    @Override
    public String getName() {
        return "mso2007detector";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public String[] process(byte[] data, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {
        String[] mimetypes = {};
        File file = null;
        try {
            file = Framework.createTempFile("magicdetector", ".xml");
            FileUtils.writeFile(file, data);
            mimetypes = guessMsoXML2007(file);
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
        return guessMsoXML2007(file);
    }

    public String[] guessMsoXML2007(File file) {
        String[] mimetype = {};
        try {
            // unzip
            ZipFile zip = new ZipFile(file);
            // look at mimetype
        } catch (IOException e) {
            log.error(e, e);
        }
        return mimetype;
    }

}
