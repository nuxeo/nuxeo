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
 * $Id: MsoXmlMimetypeSniffer.java 15407 2007-04-03 18:57:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.sf.jmimemagic.MagicDetector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;


public class MsoXmlMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(MsoXmlMimetypeSniffer.class);

    public String getDisplayName() {
        return "XML Microsoft 2003 MimeType Detector";
    }

    public String[] getHandledExtensions() {
        return new String[]{"xml"};
    }

    public String[] getHandledTypes() {
        return new String[] { "application/vnd.ms-excel", "application/msword", };
    }

    public String getName() {
        return "msoxml2003detector";
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
            mimetypes = guessMsoXml(file);
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
        return guessMsoXml(file);
    }

    public String[] guessMsoXml(File file) {
        String[] mimetype = {};
        try {
            String content = FileUtils.readFile(file);
            if (content.contains("<?mso-application progid=\"Word.Document\"?>")) {
                String[] type = { getHandledTypes()[1] };
                mimetype = type;
            } else {
                if (content.contains("<?mso-application progid=\"Excel.Sheet\"?>")) {
                    String[] type = { getHandledTypes()[0] };
                    mimetype = type;
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return mimetype;
    }

}
