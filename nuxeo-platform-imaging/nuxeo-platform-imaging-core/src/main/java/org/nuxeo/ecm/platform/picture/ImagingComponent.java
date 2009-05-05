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

package org.nuxeo.ecm.platform.picture;

import java.io.InputStream;
import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.core.libraryselector.LibrarySelector;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Max Stepanov
 *
 */
public class ImagingComponent extends DefaultComponent implements
        ImagingService {

    private static final Log log = LogFactory.getLog(ImagingComponent.class);

    private LibrarySelector librarySelector;

    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().crop(in, x, y,
                    width, height);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return in;
    }

    public InputStream resize(InputStream in, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().resize(in,
                    width, height);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return in;
    }

    public InputStream rotate(InputStream in, int angle) {
        try {
            return getLibrarySelectorService().getImageUtils().rotate(in, angle);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return in;
    }

    public Map<String, Object> getImageMetadata(InputStream in) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    in);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMetadata Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMetadata Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return null;
    }

    public Map<String, Object> getImageMetadata(File file) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    file);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMetadata Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMetadata Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return null;
    }

    public String getImageMimeType(File file) {
        try {
            return getLibrarySelectorService().getMimeUtils().getImageMimeType(file);
        }catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMime Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMime Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return null;
    }

    public String getImageMimeType(InputStream in) {
        try {
            return getLibrarySelectorService().getMimeUtils().getImageMimeType(in);
        }catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMime Class");
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMime Class");
        } catch (ClientException e) {
            log.error("ClientException");
        }
        return null;
    }

    private LibrarySelector getLibrarySelectorService() throws ClientException {
        if (librarySelector == null) {
            librarySelector = Framework.getRuntime().getService(
                    LibrarySelector.class);
        }
        if (librarySelector == null) {
            log.error("Unable to get LibrarySelector runtime service");
            throw new ClientException(
                    "Unable to get LibrarySelector runtime service");
        }
        return librarySelector;
    }


}
