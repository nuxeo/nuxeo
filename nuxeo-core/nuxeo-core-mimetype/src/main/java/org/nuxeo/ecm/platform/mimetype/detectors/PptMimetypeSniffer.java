/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: PptMimetypeSniffer.java 20310 2007-06-11 15:54:14Z lgodard $
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
import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.nuxeo.common.utils.FileUtils;

public class PptMimetypeSniffer implements MagicDetector {

    private static final Log log = LogFactory.getLog(PptMimetypeSniffer.class);

    public String getDisplayName() {
        return "PPT MimeType Detector";
    }

    public String[] getHandledExtensions() {
        return new String[] { "ppt", "pps" };
    }

    public String[] getHandledTypes() {
        return new String[] { "application/vnd.ms-powerpoint" };
    }

    public String getName() {
        return "pptdetector";
    }

    public String getVersion() {
        return "0.1";
    }

    public String[] process(byte[] data, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {

        String[] mimetypes = { "" };
        File file = null;

        try {
            file = File.createTempFile("magicdetector", ".ppt");
            FileUtils.writeFile(file, data);
            mimetypes = guessPowerpoint(file);
        } catch (IOException e) {
            log.error(e, e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }

        return mimetypes;
    }

    public String[] process(File file, int offset, int length, long bitmask, char comparator, String mimeType,
            Map params) {

        return guessPowerpoint(file);
    }

    public String[] guessPowerpoint(File file) {

        String[] mimetypes = {};

        try {
            FileInputStream stream = new FileInputStream(file);
            HSLFSlideShow ppt = new HSLFSlideShow(stream);
            SlideShow presentation = new SlideShow(ppt);

            if (presentation.getSlides().length != 0) {
                mimetypes = getHandledTypes();
            }
        } catch (FileNotFoundException e) {
            // This is not powerpoint file
            log.debug("MimeType detector : Not a powerpoint file - FileNotFoundException");
        } catch (IOException e) {
            // This is not a powerpoint file
            log.debug("MimeType detector : Not a powerpoint file - IOException");
        } catch (RuntimeException e) {
            // This is not a powerpoint file
            log.debug("MimeType detector : Not a powerpoint file - RuntimeException");
        }
        return mimetypes;
    }

}
