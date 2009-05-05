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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;
import org.nuxeo.ecm.platform.picture.core.MimeUtils;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class LibrarySelectorService extends DefaultComponent implements LibrarySelector{
    public static final String NAME = "org.nuxeo.ecm.platform.picture.core.libraryselector.LibrarySelectorService";

    public static final String LIBRARY_SELECTOR = "LibrarySelector";

    private static final Log log = LogFactory.getLog(LibrarySelectorService.class);

    private static ImageUtilsDescriptor imageUtilsDescriptor;

    private static MetadataUtilsDescriptor metadataUtilsDescriptor;

    private static MimeUtilsDescriptor mimeUtilsDescriptor;

    @Override
    public void activate(ComponentContext context) {

    }

    @Override
    public void deactivate(ComponentContext context) {
        imageUtilsDescriptor = null;
        metadataUtilsDescriptor = null;
        mimeUtilsDescriptor = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(LIBRARY_SELECTOR)) {
            LibrarySelectorServiceDescriptor libraryDesc = (LibrarySelectorServiceDescriptor) contribution;
            registerPictureAdapter(libraryDesc, contributor);
        } else {
            log.error("Extension point " + extensionPoint + "is unknown");
        }

    }

    public static void registerPictureAdapter(
            LibrarySelectorServiceDescriptor libraryDesc,
            ComponentInstance contributor) {
        imageUtilsDescriptor = libraryDesc.getImageUtils();
        mimeUtilsDescriptor = libraryDesc.getMimeUtils();
        metadataUtilsDescriptor = libraryDesc.getMetadataUtils();
        log.debug("Using " + imageUtilsDescriptor.getName()
                + " for ImageUtils.\n" + "Using "
                + mimeUtilsDescriptor.getName() + " for MimeUtils.\n"
                + "Using " + metadataUtilsDescriptor.getName()
                + " for MeadataUtils.\n");
    }

    public ImageUtils getImageUtils() throws InstantiationException,
            IllegalAccessException {
        return imageUtilsDescriptor.getNewInstance();
    }

    public MimeUtils getMimeUtils() throws InstantiationException,
            IllegalAccessException {
        return mimeUtilsDescriptor.getNewInstance();
    }

    public MetadataUtils getMetadataUtils()
            throws InstantiationException, IllegalAccessException {
        return metadataUtilsDescriptor.getNewInstance();
    }
}
