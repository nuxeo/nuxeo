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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import java.util.MissingResourceException;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.i18n.client.Dictionary;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class PreviewSettings {

    private static final String PREVIEW_SETTINGS = "previewSettings";

    private static final String IMAGE_ONLY = "imageOnly";

    private static final String MULTI_IMAGE_ANNOTATION = "multiImageAnnotation";

    private static final String XPOINTER_FILTER_PATH = "xPointerFilterPath";

    private static final String POINTER_ADAPTER = "pointerAdapter";

    private static final String ANNOTATION_DECORATOR_FUNCTION = "annotationDecoratorFunction";

    private static PreviewSettings INSTANCE;

    private Dictionary dictionary;

    public static PreviewSettings getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new PreviewSettings();
            } catch (MissingResourceException e) {
                Log.debug("Preview Settings dictionary not found");
                INSTANCE = null;
            }
        }
        return INSTANCE;
    }

    public PreviewSettings() {
        dictionary = Dictionary.getDictionary(PREVIEW_SETTINGS);
    }

    private String get(String key) {
        try {
            return dictionary.get(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public boolean isImageOnly() {
        String imageOnly = get(IMAGE_ONLY);
        return imageOnly != null ? Boolean.parseBoolean(imageOnly) : false;
    }

    public boolean isMultiImageAnnotation() {
        String multiImageAnnotation = get(MULTI_IMAGE_ANNOTATION);
        return multiImageAnnotation != null ? Boolean.parseBoolean(multiImageAnnotation) : false;
    }

    public String getXPointerFilterPath() {
        return get(XPOINTER_FILTER_PATH);
    }

    public String getPointerAdapter() {
        return get(POINTER_ADAPTER);
    }

    public String getAnnotationDecoratorFunction() {
        return get(ANNOTATION_DECORATOR_FUNCTION);
    }

}
