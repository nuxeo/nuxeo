/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import java.util.MissingResourceException;

import com.google.gwt.i18n.client.Dictionary;

/**
 * @author Alexandre Russel
 */
public class AnnotationConfiguration {
    private static final String ANNOTATION_CSS_URL = "annotationCssUrl";

    private static final String ANNOTEA_SERVER_URL = "annoteaServerUrl";

    private static final String PREVIEW_URL = "previewUrl";

    private static final String DOCUMENT_URL = "documentUrl";

    /**
     * @since 5.7
     */
    private static final String DATE_FORMAT_PATTERN = "dateFormatPattern";

    private static final String ANNOTATION_CONFIGURATION = "annotationConfiguration";

    private static final AnnotationConfiguration INSTANCE;

    private String annoteaServerUrl;

    private String annotationCssUrl;

    private String previewUrl;

    private String documentUrl;

    /**
     * @since 5.7
     */
    private String dateFormatPattern;

    static {
        INSTANCE = new AnnotationConfiguration();
        INSTANCE.loadConfiguration();
    }

    public static AnnotationConfiguration getInstance() {
        return INSTANCE;
    }

    public String getAnnotationCssUrl() {
        return annotationCssUrl;
    }

    public void setAnnotationCssUrl(String annotationCssUrl) {
        this.annotationCssUrl = annotationCssUrl;
    }

    public String getAnnoteaServerUrl() {
        return annoteaServerUrl;
    }

    public void setAnnoteaServerUrl(String annoteaServerUrl) {
        this.annoteaServerUrl = annoteaServerUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    /**
     * Returns the configured date format, if any.
     *
     * @since 5.7
     */
    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    private void loadConfiguration() {
        Dictionary dictionary = Dictionary.getDictionary(ANNOTATION_CONFIGURATION);
        annoteaServerUrl = dictionary.get(ANNOTEA_SERVER_URL);
        annotationCssUrl = dictionary.get(ANNOTATION_CSS_URL);
        previewUrl = dictionary.get(PREVIEW_URL);
        documentUrl = dictionary.get(DOCUMENT_URL);
        try {
            // this one is optional
            dateFormatPattern = dictionary.get(DATE_FORMAT_PATTERN);
        } catch (MissingResourceException e) {
            dateFormatPattern = null;
        }
    }
}
