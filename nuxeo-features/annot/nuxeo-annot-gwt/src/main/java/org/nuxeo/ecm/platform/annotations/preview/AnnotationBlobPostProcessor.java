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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.preview;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.preview.adapter.BlobPostProcessor;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationBlobPostProcessor implements BlobPostProcessor {

    private static final Log log = LogFactory.getLog(AnnotationBlobPostProcessor.class);

    protected static final int BUFFER_SIZE = 4096 * 16;

    protected static final String GWT_LOCALE = "<meta name=\"gwt:property\" content=\"locale=%s\" />";

    protected static final String ANNOTATION_MODULE_JS = "<script type=\"text/javascript\" src='"
            + VirtualHostHelper.getContextPathProperty()
            + "/org.nuxeo.ecm.platform.annotations.gwt.AnnotationFrameModule/org.nuxeo.ecm.platform.annotations.gwt.AnnotationFrameModule.nocache.js'></script>";

    protected static final String INTERNET_EXPLORER_RANGE_JS = "<script type=\"text/javascript\" src='"
            + VirtualHostHelper.getContextPathProperty() + "/scripts/InternetExplorerRange.js'></script>";

    protected Pattern headPattern = Pattern.compile("(.*)(<head>)(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected Pattern htmlPattern = Pattern.compile("(.*)(<html>)(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected Pattern charsetPattern = Pattern.compile("(.*) charset=(.*?)\"(.*)", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);

    public Blob process(Blob blob) {
        String mimetype = blob.getMimeType();
        if (mimetype == null || !mimetype.startsWith("text/")) {
            // blob does not carry HTML payload hence there is no need to try to
            // inject HTML metadata
            return blob;
        }
        try {
            String encoding = null;
            if (blob.getEncoding() == null) {
                Matcher m = charsetPattern.matcher(blob.getString());
                if (m.matches()) {
                    encoding = m.group(2);
                }
            } else {
                encoding = blob.getEncoding();
            }

            String blobAsString = getBlobAsString(blob, encoding);
            String processedBlob = addAnnotationModule(blobAsString);

            blob = Blobs.createBlob(processedBlob, blob.getMimeType(), encoding, blob.getFilename());
            blob.setDigest(DigestUtils.md5Hex(processedBlob));
        } catch (IOException e) {
            log.debug("Unable to process Blob", e);
        }
        return blob;
    }

    protected String getBlobAsString(Blob blob, String encoding) throws IOException {
        if (encoding == null) {
            return blob.getString();
        }
        Reader reader = new InputStreamReader(blob.getStream(), encoding);
        return readString(reader);
    }

    protected String addAnnotationModule(String blob) {
        LocaleSelector localeSelector = LocaleSelector.instance();
        StringBuilder sb = new StringBuilder();
        Matcher m = headPattern.matcher(blob);
        if (m.matches()) {
            sb.append(m.group(1));
            sb.append(m.group(2));
            if (localeSelector != null) {
                sb.append(String.format(GWT_LOCALE, localeSelector.getLocaleString()));
            }
            sb.append(INTERNET_EXPLORER_RANGE_JS);
            sb.append(ANNOTATION_MODULE_JS);
            sb.append(m.group(3));
        } else {
            m = htmlPattern.matcher(blob);
            if (m.matches()) {
                sb.append(m.group(1));
                sb.append(m.group(2));
                sb.append("<head>");
                if (localeSelector != null) {
                    sb.append(String.format(GWT_LOCALE, localeSelector.getLocaleString()));
                }
                sb.append(INTERNET_EXPLORER_RANGE_JS);
                sb.append(ANNOTATION_MODULE_JS);
                sb.append("</head>");
                sb.append(m.group(3));
            } else {
                log.debug("Unable to inject Annotation module javascript");
                sb.append(blob);
            }
        }
        return sb.toString();
    }

    public static String readString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(BUFFER_SIZE);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer, 0, BUFFER_SIZE)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

}
