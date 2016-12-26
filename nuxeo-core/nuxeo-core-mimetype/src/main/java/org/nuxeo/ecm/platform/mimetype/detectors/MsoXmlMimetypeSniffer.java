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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

import net.sf.jmimemagic.MagicDetector;

public class MsoXmlMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(MsoXmlMimetypeSniffer.class);

    @Override
    public String getDisplayName() {
        return "XML Microsoft 2003 MimeType Detector";
    }

    @Override
    public String[] getHandledExtensions() {
        return new String[] { "xml" };
    }

    @Override
    public String[] getHandledTypes() {
        return new String[] { "application/vnd.ms-excel", "application/msword", };
    }

    @Override
    public String getName() {
        return "msoxml2003detector";
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
            FileUtils.writeByteArrayToFile(file, data);
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

    @Override
    public String[] process(File file, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {
        return guessMsoXml(file);
    }

    public String[] guessMsoXml(File file) {
        String[] mimetype = {};
        try {
            String content = FileUtils.readFileToString(file);
            if (content.contains("<?mso-application progid=\"Word.Document\"?>")) {
                String[] type = { getHandledTypes()[1] };
                mimetype = type;
            } else {
                if (content.contains("<?mso-application progid=\"Excel.Sheet\"?>")) {
                    String[] type = { getHandledTypes()[0] };
                    mimetype = type;
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
        return mimetype;
    }

}
