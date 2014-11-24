/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.api;

import static org.apache.commons.logging.LogFactory.getLog;

import org.apache.commons.logging.Log;

/**
 * Object to store the definition of a picture template, to be used when
 * computing views for a given image.
 *
 * @deprecated since 7.1. Use
 *             {@link org.nuxeo.ecm.platform.picture.api.PictureConversion}.
 */
@Deprecated
public class PictureTemplate extends PictureConversion {

    private static final Log log = getLog(PictureTemplate.class);

    public PictureTemplate(String title, String description, String tag,
            Integer maxSize) {
        super(title, description, tag, maxSize, -1, null, true);
        log.warn("PictureTemplate is deprecated since 7.1, please use PictureConversion instead");
    }
}
