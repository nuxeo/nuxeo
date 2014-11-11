/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import com.google.gwt.i18n.client.Dictionary;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationConfiguration {
    private static final String ANNOTATION_CSS_URL = "annotationCssUrl";

    private static final String ANNOTEA_SERVER_URL = "annoteaServerUrl";

    private static final String PREVIEW_URL = "previewUrl";

    private static final String DOCUMENT_URL = "documentUrl";

    private static final String ANNOTATION_CONFIGURATION = "annotationConfiguration";

    private static final AnnotationConfiguration INSTANCE;

    private String annoteaServerUrl;

    private String annotationCssUrl;

    private String previewUrl;

    private String documentUrl;

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

    private void loadConfiguration() {
        Dictionary dictionary = Dictionary.getDictionary(ANNOTATION_CONFIGURATION);
        annoteaServerUrl = dictionary.get(ANNOTEA_SERVER_URL);
        annotationCssUrl = dictionary.get(ANNOTATION_CSS_URL);
        previewUrl = dictionary.get(PREVIEW_URL);
        documentUrl = dictionary.get(DOCUMENT_URL);
    }
}
