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

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Max Stepanov
 *
 */
public interface ImagingService {

    /**
     * crop image
     *
     * @param in
     * @param x
     * @param y
     * @param width
     * @param height
     * @return resized image file created in temporary folder
     */
    InputStream crop(InputStream in, int x, int y, int width, int height);

    /**
     * Resize image
     *
     * @param in
     * @param width
     * @param height
     * @return resized image file created in temporary folder
     */
    InputStream resize(InputStream in, int width, int height);

    /**
     * Rotate image
     *
     * @param in
     * @param angle
     * @return
     */
    InputStream rotate(InputStream in, int angle);

    /**
     * Retrieve metadata from an image
     *
     * @param in
     * @return metadata
     */
    Map<String, Object> getImageMetadata(InputStream in);
    Map<String, Object> getImageMetadata(File file);

    /**
     * return file mime-type
     *
     * @param file
     * @return
     */
    String getImageMimeType(File file);
    String getImageMimeType(InputStream in);
}
