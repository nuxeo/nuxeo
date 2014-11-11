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
 * $Id: MsoXML2007MimetypeSniffer.java 20591 2007-06-16 16:26:40Z sfermigier $
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

public class MsoXML2007MimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(MsoXML2007MimetypeSniffer.class);

    public String getDisplayName() {
        return "Microsoft 2007 files MimeType Detector";
    }

    public String[] getHandledExtensions() {
        return new String[] {
                "docm", "docx", "dotm", "dotx", "ppsm", "ppsx",
                "pptm", "pptx", "xlsb", "xlsm", "xlsx", "xps" };
    }

    public String[] getHandledTypes() {
        return new String[] {
                "application/vnd.ms-word.document.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-word.template.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                "application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
                "application/vnd.ms-excel.sheet.macroEnabled.12",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-xpsdocument", };
    }

    public String getName() {
        return "mso2007detector";
    }

    public String getVersion() {
        return "0.1";
    }

    public String[] process(byte[] data, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {
        String[] mimetypes = {};
        File file = null;
        try {
            file = File.createTempFile("magicdetector", ".xml");
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

    public String[] process(File file, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {
        return guessMsoXML2007(file);
    }

    public String[] guessMsoXML2007(File file) {
        String[] mimetype = {};
        try {
            // unzip
            ZipFile zip = new ZipFile(file);
            // look at mimetype
        } catch (Exception e) {
            log.error(e, e);
        }
        return mimetype;
    }

}
