/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.core.libraryselector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class LibrarySelectorService extends DefaultComponent implements LibrarySelector {

    private static final Logger log = LogManager.getLogger(LibrarySelectorService.class);

    public static final String LIBRARY_SELECTOR = "LibrarySelector";

    protected ImageUtils imageUtils;

    protected MetadataUtils metadataUtils;

    @Override
    public void deactivate(ComponentContext context) {
        imageUtils = null;
        metadataUtils = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(LIBRARY_SELECTOR)) {
            LibrarySelectorServiceDescriptor libraryDescriptor = (LibrarySelectorServiceDescriptor) contribution;
            registerLibrarySelector(libraryDescriptor);
        } else {
            log.error("Extension point: {} is unknown", extensionPoint);
        }
    }

    public void registerLibrarySelector(LibrarySelectorServiceDescriptor libraryDescriptor) {
        registerImageUtils(libraryDescriptor.getImageUtils());
        registerMetadataUtils(libraryDescriptor.getMetadataUtils());
    }

    protected void registerImageUtils(ImageUtilsDescriptor imageUtilsDescriptor) {
        if (imageUtilsDescriptor == null) {
            return;
        }
        imageUtils = imageUtilsDescriptor.getNewInstance();
        log.debug("Using: {} for ImageUtils.", imageUtils.getClass().getName());
    }

    protected void registerMetadataUtils(MetadataUtilsDescriptor metadataUtilsDescriptor) {
        if (metadataUtilsDescriptor == null) {
            return;
        }
        metadataUtils = metadataUtilsDescriptor.getNewInstance();
        log.debug("Using: {} for MetadataUtils.", metadataUtils.getClass().getName());
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
