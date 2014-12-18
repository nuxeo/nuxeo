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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for the information returned by the Identify ImageMagick command.
 *
 * @author tiry
 */
public class ImageInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String DEPTH = "depth";

    public static final String FORMAT = "format";

    public static final String COLOR_SPACE = "colorSpace";

    protected int width;

    protected int height;

    protected int depth;

    protected String format;

    /** @since 5.9.5 */
    protected String colorSpace;

    protected String filePath;

    public static ImageInfo fromMap(Map<String, Serializable> map) {
        if (map == null) {
            return null;
        }

        ImageInfo info = new ImageInfo();
        Long width = (Long) map.get(WIDTH);
        if (width != null) {
            info.width = width.intValue();
        }
        Long height = (Long) map.get(HEIGHT);
        if (height != null) {
            info.height = height.intValue();
        }
        Long depth = (Long) map.get(DEPTH);
        if (depth != null) {
            info.depth = depth.intValue();
        }
        info.format = (String) map.get(FORMAT);
        info.colorSpace = (String) map.get(COLOR_SPACE);
        return info;
    }

    public ImageInfo() {
    }

    public ImageInfo(String width, String height, String format, String filePath) {
        this.width = Integer.parseInt(width);
        this.height = Integer.parseInt(height);
        this.format = format;
        this.filePath = filePath;
    }

    public ImageInfo(String width, String height, String format, String depth, String filePath) {
        this(width, height, format, filePath);
        this.depth = Integer.parseInt(depth);
    }

    /** @since 5.9.5 */
    public ImageInfo(String width, String height, String format, String depth, String colorSpace, String filePath) {
        this(width, height, format, filePath);
        this.depth = Integer.parseInt(depth);
        this.colorSpace = colorSpace;
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

    /** @since 5.9.5 */
    public String getColorSpace() {
        return colorSpace;
    }

    /** @since 5.9.5 */
    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    /**
     * Returns a {@code Map} of attributes for this {@code ImageInfo}.
     * <p>
     * Used when saving this {@code ImageInfo} to a {@code DocumentModel} property.
     *
     * @since 7.1
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(WIDTH, width);
        map.put(HEIGHT, height);
        map.put(DEPTH, depth);
        map.put(FORMAT, format);
        map.put(COLOR_SPACE, colorSpace);
        return map;
    }
}
