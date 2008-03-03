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

package org.nuxeo.ecm.platform.imaging.api;


/**
 * @author Max Stepanov
 *
 */
public interface IMetadata {

    static final String META_WIDTH = "width";
    static final String META_HEIGHT = "height";
    /* EXIF */
    static final String META_DESCRIPTION = "description";
    static final String META_COMMENT = "comment";
    static final String META_EQUIPMENT = "equipment";
    static final String META_ORIGINALDATE = "originalDate";
    static final String META_HRESOLUTION = "horizontalResolution";
    static final String META_VRESOLUTION = "verticalResolution";
    static final String META_COPYRIGHT = "copyright";
    static final String META_EXPOSURE = "exposure";
    static final String META_ISOSPEED = "ISOspeed";
    static final String META_FOCALLENGTH = "focalLength";
    static final String META_COLORSPACE = "colorSpace";
    static final String META_WHITEBALANCE = "whiteBalance";
    static final String META_ICCPROFILE = "iccProfile";
    /* IPTC */
    static final String META_BYLINE = "byLine";
    static final String META_CAPTION = "caption";
    static final String META_CATEGORY = "category";
    static final String META_CITY = "city";
    static final String META_COUNTRY = "country";
    static final String META_CREDIT = "credit";
    static final String META_DATE = "originalDate"; /* matches META_ORIGINALDATE */
    static final String META_HEADLINE = "headline";
    static final String META_LANGUAGE = "language";
    static final String META_OBJECTNAME = "objectName";
    static final String META_SUPPLEMENTALCATEGORIES = "supplementalCategories";
    static final String META_SOURCE = "source";

}
