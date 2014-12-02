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
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class LibrarySelectorService extends DefaultComponent implements
        LibrarySelector {

    public static final String LIBRARY_SELECTOR = "LibrarySelector";

    private static final Log log = LogFactory.getLog(LibrarySelectorService.class);

    protected ImageUtils imageUtils;

    protected MetadataUtils metadataUtils;

    @Override
    public void deactivate(ComponentContext context) {
        imageUtils = null;
        metadataUtils = null;
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
    }

    protected void registerImageUtils(ImageUtilsDescriptor imageUtilsDescriptor) {
        if (imageUtilsDescriptor == null) {
            return;
        }
        imageUtils = imageUtilsDescriptor.getNewInstance();
        log.debug("Using " + imageUtils.getClass().getName()
                + " for ImageUtils.");
    }

    protected void registerMetadataUtils(
            MetadataUtilsDescriptor metadataUtilsDescriptor) {
        if (metadataUtilsDescriptor == null) {
            return;
        }
        metadataUtils = metadataUtilsDescriptor.getNewInstance();
        log.debug("Using " + metadataUtils.getClass().getName()
                + " for MetadataUtils.");
    }

    @Override
    public ImageUtils getImageUtils() {
        return imageUtils;
    }

    @Override
    public MetadataUtils getMetadataUtils() {
        return metadataUtils;
    }

}
