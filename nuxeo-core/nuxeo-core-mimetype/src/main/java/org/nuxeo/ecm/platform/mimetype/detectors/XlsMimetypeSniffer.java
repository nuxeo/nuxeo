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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.nuxeo.runtime.api.Framework;

import net.sf.jmimemagic.MagicDetector;

public class XlsMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(XlsMimetypeSniffer.class);

    @Override
    public String getDisplayName() {
        return "XLS MimeType Detector";
    }

    @Override
    public String[] getHandledExtensions() {
        return new String[] { "xls" };
    }

    @Override
    public String[] getHandledTypes() {
        return new String[] { "application/vnd.ms-excel", "application/msexcel", "application/vnd.microsoft-excel" };
    }

    @Override
    public String getName() {
        return "xlsdetector";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public String[] process(byte[] data, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {

        String[] mimetypes = { "" };
        File file = null;

        try {
            file = Framework.createTempFile("magicdetector", ".xls");
            FileUtils.writeByteArrayToFile(file, data);
            mimetypes = guessExcel(file);
        } catch (IOException e) {
            log.error(e, e);
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

        return guessExcel(file);
    }

    public String[] guessExcel(File file) {

        String[] mimetypes = {};

        try (FileInputStream stream = new FileInputStream(file); //
                HSSFWorkbook workbook = new HSSFWorkbook(stream)) {
            if (workbook.getNumberOfSheets() != 0) {
                mimetypes = getHandledTypes();
            }
        } catch (FileNotFoundException e) {
            // This is not an excel file
            log.debug("MimeType detector : Not an excel file");
        } catch (IOException e) {
            // This is not an excel file
            log.debug("MimeType detector : Not an excel file");
        } catch (IllegalArgumentException e) {
            log.debug("MimeType detector : Not an excel file");
        }

        return mimetypes;
    }

}
