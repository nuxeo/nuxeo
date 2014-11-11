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
import org.nuxeo.ecm.platform.picture.core.mistral.MistralImageUtils;
import org.nuxeo.ecm.platform.picture.core.mistral.MistralMetadataUtils;
import org.nuxeo.ecm.platform.picture.core.mistral.MistralMimeUtils;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class LibrarySelectorService extends DefaultComponent implements LibrarySelector{

    public static final String LIBRARY_SELECTOR = "LibrarySelector";

    private static final Log log = LogFactory.getLog(LibrarySelectorService.class);

    protected static final ImageUtils DEFAULT_IMAGE_UTILS = new MistralImageUtils();

    protected static final MetadataUtils DEFAULT_METADATA_UTILS = new MistralMetadataUtils();

    protected static final MimeUtils DEFAULT_MIME_UTILS = new MistralMimeUtils();

    protected ImageUtils imageUtils;

    protected MetadataUtils metadataUtils;

    protected MimeUtils mimeUtils;

    @Override
    public void deactivate(ComponentContext context) {
        imageUtils = null;
        metadataUtils = null;
        mimeUtils = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(LIBRARY_SELECTOR)) {
            LibrarySelectorServiceDescriptor libraryDescriptor = (LibrarySelectorServiceDescriptor) contribution;
            registerLibrarySelector(libraryDescriptor);
        } else {
            log.error("Extension point " + extensionPoint + "is unknown");
        }
    }

    public void registerLibrarySelector(
            LibrarySelectorServiceDescriptor libraryDescriptor) {
        registerImageUtils(libraryDescriptor.getImageUtils());
        registerMetadataUtils(libraryDescriptor.getMetadataUtils());
        registerMimeUtils(libraryDescriptor.getMimeUtils());
    }

    protected void registerImageUtils(ImageUtilsDescriptor imageUtilsDescriptor) {
        try {
            imageUtils = imageUtilsDescriptor.getNewInstance();
        } catch (Exception e) {
            imageUtils = DEFAULT_IMAGE_UTILS;
        }
        if (!imageUtils.isAvailable()) {
            imageUtils = DEFAULT_IMAGE_UTILS;
        }
        log.debug("Using " + imageUtils.getClass().getName() + " for ImageUtils.");
    }

    protected void registerMetadataUtils(MetadataUtilsDescriptor metadataUtilsDescriptor) {
        try {
            metadataUtils = metadataUtilsDescriptor.getNewInstance();
        } catch (Exception e) {
            metadataUtils = DEFAULT_METADATA_UTILS;
        }
        log.debug("Using " + metadataUtils.getClass().getName() + " for MetadataUtils.");
    }

    protected void registerMimeUtils(MimeUtilsDescriptor mimeUtilsDescriptor) {
        try {
            mimeUtils = mimeUtilsDescriptor.getNewInstance();
        } catch (Exception e) {
            mimeUtils = DEFAULT_MIME_UTILS;
        }
        log.debug("Using " + mimeUtils.getClass().getName() + " for MimeUtils.");
    }

    public ImageUtils getImageUtils() {
        return imageUtils;
    }

    public MimeUtils getMimeUtils() {
        return mimeUtils;
    }

    public MetadataUtils getMetadataUtils() {
        return metadataUtils;
    }

}
