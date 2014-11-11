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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("LibrarySelector")
public class LibrarySelectorServiceDescriptor {

    @XNode("ImageUtils")
    public ImageUtilsDescriptor imageUtils;

    @XNode("MimeUtils")
    public MimeUtilsDescriptor mimeUtils;

    @XNode("MetadataUtils")
    public MetadataUtilsDescriptor metadataUtils;

    public ImageUtilsDescriptor getImageUtils() {
        return imageUtils;
    }

    public MimeUtilsDescriptor getMimeUtils() {
        return mimeUtils;
    }

    public MetadataUtilsDescriptor getMetadataUtils() {
        return metadataUtils;
    }

}
