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
 *
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
