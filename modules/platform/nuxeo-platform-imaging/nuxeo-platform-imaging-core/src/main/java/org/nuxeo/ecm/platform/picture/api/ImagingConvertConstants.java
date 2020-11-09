/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.api;

/**
 * @author Max Stepanov
 */
public class ImagingConvertConstants {

    /** Operations */
    public static final String OPERATION_RESIZE = "pictureResize";

    public static final String OPERATION_ROTATE = "pictureRotation";

    public static final String OPERATION_CROP = "pictureCrop";

    /**
     * @since 8.4
     */
    public static final String OPERATION_CONVERT_TO_PDF = "pictureConvertToPDF";

    /** Operation specific options */
    public static final String OPTION_RESIZE_WIDTH = "width";

    public static final String OPTION_RESIZE_HEIGHT = "height";

    public static final String OPTION_RESIZE_DEPTH = "depth";

    public static final String OPTION_CROP_X = "x";

    public static final String OPTION_CROP_Y = "y";

    public static final String OPTION_ROTATE_ANGLE = "angle";

    public static final String CONVERSION_FORMAT = "conversionFormat";

    public static final String JPEG_CONVERSATION_FORMAT = "jpg";

    private ImagingConvertConstants() {
    }

}
