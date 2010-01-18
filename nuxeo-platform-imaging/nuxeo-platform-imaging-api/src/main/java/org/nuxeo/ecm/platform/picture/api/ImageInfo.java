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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.picture.api;

import java.io.File;

/**
 * Wrapper class for the informations returned by the Identify ImageMagick
 * command.
 *
 * @author tiry
 */
public class ImageInfo {

    protected int width;

    protected int height;

    protected int depth;

    protected String format;

    protected String filePath;

    public ImageInfo(String width, String height, String format, String filePath) {
        this.width = Integer.parseInt(width);
        this.height = Integer.parseInt(height);
        this.format = format;
        this.filePath = filePath;
    }

    public ImageInfo(String width, String height, String format, String depth,
            String filePath) {
        this(width, height, format, filePath);
        this.depth = Integer.parseInt(depth);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return width + "x" + height + "-" + format;
    }

    public String getFilePath() {
        return filePath;
    }

    public File getFile() {
        return new File(filePath);
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

}
