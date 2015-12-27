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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class LibrarySelectorService extends DefaultComponent implements LibrarySelector {

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
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(LIBRARY_SELECTOR)) {
            LibrarySelectorServiceDescriptor libraryDescriptor = (LibrarySelectorServiceDescriptor) contribution;
            registerLibrarySelector(libraryDescriptor);
        } else {
            log.error("Extension point " + extensionPoint + "is unknown");
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
        log.debug("Using " + imageUtils.getClass().getName() + " for ImageUtils.");
    }

    protected void registerMetadataUtils(MetadataUtilsDescriptor metadataUtilsDescriptor) {
        if (metadataUtilsDescriptor == null) {
            return;
        }
        metadataUtils = metadataUtilsDescriptor.getNewInstance();
        log.debug("Using " + metadataUtils.getClass().getName() + " for MetadataUtils.");
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
