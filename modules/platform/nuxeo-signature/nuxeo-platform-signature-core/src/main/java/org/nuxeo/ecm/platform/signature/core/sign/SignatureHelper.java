/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vincent Dutat
 */
package org.nuxeo.ecm.platform.signature.core.sign;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.SigningDisposition;
import org.nuxeo.runtime.api.Framework;

public class SignatureHelper {

    private static final Log log = LogFactory.getLog(SignatureHelper.class);

    /**
     * If this system property is set to "true", then signature will use PDF/A.
     */
    public static final String SIGNATURE_USE_PDFA_PROP = "org.nuxeo.ecm.signature.pdfa";

    /**
     * Signature disposition for PDF files. Can be "replace", "archive" or "attach".
     */
    public static final String SIGNATURE_DISPOSITION_PDF = "org.nuxeo.ecm.signature.disposition.pdf";

    /**
     * Signature disposition for non-PDF files. Can be "replace", "archive" or "attach".
     */
    public static final String SIGNATURE_DISPOSITION_NOTPDF = "org.nuxeo.ecm.signature.disposition.notpdf";

    public static final String SIGNATURE_ARCHIVE_FILENAME_FORMAT_PROP = "org.nuxeo.ecm.signature.archive.filename.format";

    /** Used with {@link SimpleDateFormat}. */
    public static final String DEFAULT_ARCHIVE_FORMAT = " ('archive' yyyy-MM-dd HH:mm:ss)";

    private SignatureHelper() {}

    public static boolean getPDFA() {
        return Framework.isBooleanPropertyTrue(SIGNATURE_USE_PDFA_PROP);
    }

    public static SigningDisposition getDisposition(boolean originalIsPdf) {
        String disp;
        if (originalIsPdf) {
            disp = Framework.getProperty(SIGNATURE_DISPOSITION_PDF, SigningDisposition.ARCHIVE.name());
        } else {
            disp = Framework.getProperty(SIGNATURE_DISPOSITION_NOTPDF, SigningDisposition.ATTACH.name());
        }
        try {
            return Enum.valueOf(SigningDisposition.class, disp.toUpperCase());
        } catch (RuntimeException e) {
            log.warn("Invalid signing disposition: " + disp);
            return SigningDisposition.ATTACH;
        }
    }

    public static String getArchiveFilename(String filename) {
        String format = Framework.getProperty(SIGNATURE_ARCHIVE_FILENAME_FORMAT_PROP, DEFAULT_ARCHIVE_FORMAT);
        return FilenameUtils.getBaseName(filename) + new SimpleDateFormat(format).format(new Date()) + "."
                + FilenameUtils.getExtension(filename);
    }
}
