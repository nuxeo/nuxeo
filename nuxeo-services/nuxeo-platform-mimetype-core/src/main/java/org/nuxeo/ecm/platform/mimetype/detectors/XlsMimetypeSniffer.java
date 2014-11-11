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
 * $Id: XlsMimetypeSniffer.java 20310 2007-06-11 15:54:14Z lgodard $
 */

package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import net.sf.jmimemagic.MagicDetector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.nuxeo.common.utils.FileUtils;


public class XlsMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(XlsMimetypeSniffer.class);

    public String getDisplayName() {
        return "XLS MimeType Detector";
    }

    public String[] getHandledExtensions() {
        return new String[] { "xls" };
    }

    public String[] getHandledTypes() {
        return new String[] {
                "application/vnd.ms-excel",
                "application/msexcel",
                "application/vnd.microsoft-excel" };
    }

    public String getName() {
        return "xlsdetector";
    }

    public String getVersion() {
        return "0.1";
    }

    public String[] process(byte[] data, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {

        String[] mimetypes = { "" };
        File file = null;

        try {
            file = File.createTempFile("magicdetector", ".xls");
            FileUtils.writeFile(file, data);
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

    public String[] process(File file, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {

        return guessExcel(file);
    }

    public String[] guessExcel(File file) {

        String[] mimetypes = {};

        try {
            FileInputStream stream = new FileInputStream(file);
            HSSFWorkbook workbook = new HSSFWorkbook(stream);
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
        } catch (Exception e) {
            log.error(e, e);
        }

        return mimetypes;
    }

}
