/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.api;

import java.io.File;
import java.io.IOException;

public interface WatermarkService {

    /**
     * Method used to return the default image file that is used to watermark other
     * images files.
     *
     * @return - return the watermark image file
     * @throws IOException
     */
    File getDefaultWatermarkFile() throws IOException;

    /**
     * Performs the watermark process using the information received as
     * parameters.
     *
     * @param watermarkFilePath
     *            - the path of the watermark file, which will be used to
     *            watermark other images
     * @param watermarkWidth
     *            - the width of the watermark
     * @param watermarkHeight
     *            - the height of the watermark
     * @param inputFilePath
     *            - the path to the image file that will be watermarked
     * @param outputFilePath
     *            - the path to file that will result after the watermark
     *            process
     * @return the watermarked image file that results from the watermark
     *         process
     * @throws Exception
     */
    File performWatermarkOnFile(String watermarkFilePath,
            Integer watermarkWidth, Integer watermarkHeight,
            String inputFilePath, String outputFilePath) throws Exception;

    /**
     * Performs the watermark process using the information received as
     * parameters. The default watermark image file will be used.
     *
     * @param inputFilePath
     *            - the path to the image file that will be watermarked
     * @return the watermarked image file that results from the watermark
     *         process
     * @throws Exception
     */
    File performWatermarkOnFile(File inputFilePath) throws Exception;
}
