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
        return new String[] {
                "application/vnd.ms-powerpoint"};
    }

    public String getName() {
        return "pptdetector";
    }

    public String getVersion() {
        return "0.1";
    }

    public String[] process(byte[] data, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {

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

    public String[] process(File file, int offset, int length, long bitmask,
            char comparator, String mimeType, Map params) {

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
        } catch (Exception e) {
            log.error(e, e);
        }
        return mimetypes;
    }

}
