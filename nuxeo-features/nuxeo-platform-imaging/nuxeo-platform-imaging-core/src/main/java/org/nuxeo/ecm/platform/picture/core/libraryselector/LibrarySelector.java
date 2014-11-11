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
 */

package org.nuxeo.ecm.platform.picture.core.libraryselector;

import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;
import org.nuxeo.ecm.platform.picture.core.MimeUtils;

public interface LibrarySelector {

    ImageUtils getImageUtils() throws InstantiationException,
            IllegalAccessException;

    /**
     * @deprecated since 5.5. ImagingService use MimetypeRegistry service to get
     *             the mime type of an image.
     */
    @Deprecated
    MimeUtils getMimeUtils() throws InstantiationException,
            IllegalAccessException;

    MetadataUtils getMetadataUtils() throws InstantiationException,
            IllegalAccessException;
}
