/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
