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
package org.nuxeo.ecm.platform.imaging.transform.api;

/**
 * @author Max Stepanov
 *
 */
public interface ImagingTransformPlugin {

    static final String OPTION_OPERATION = "operation";

    /** Operations */
    static final String OPERATION_RESIZE = "resize";

    static final String OPERATION_ROTATE = "rotate";

    static final String OPERATION_CROP = "crop";

    /** Operation specific options */
    static final String OPTION_RESIZE_WIDTH = "width";

    static final String OPTION_RESIZE_HEIGHT = "height";

    static final String OPTION_CROP_X = "x";

    static final String OPTION_CROP_Y = "y";

    static final String OPTION_ROTATE_ANGLE = "angle";

}
