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

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingConfigurationDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.core.libraryselector.LibrarySelector;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Max Stepanov
 */
public class ImagingComponent extends DefaultComponent implements
        ImagingService {

    private static final Log log = LogFactory.getLog(ImagingComponent.class);

    public static final String CONFIGURATION_PARAMETERS_EP = "configuration";

    protected Map<String, String> configurationParameters = new HashMap<String, String>();

    private LibrarySelector librarySelector;

    @Deprecated
    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().crop(in, x, y,
                    width, height);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return in;
    }

    @Deprecated
    public InputStream resize(InputStream in, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().resize(in,
                    width, height);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return in;
    }

    @Deprecated
    public InputStream rotate(InputStream in, int angle) {
        try {
            return getLibrarySelectorService().getImageUtils().rotate(in, angle);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return in;
    }

    public Blob crop(Blob blob, int x, int y, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().crop(blob, x, y,
                    width, height);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return blob;
    }

    public Blob resize(Blob blob, String finalFormat, int width, int height,
            int depth) {
        try {
            return getLibrarySelectorService().getImageUtils().resize(blob,
                    finalFormat, width, height, depth);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return blob;
    }

    public Blob rotate(Blob blob, int angle) {
        try {
            return getLibrarySelectorService().getImageUtils().rotate(blob,
                    angle);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageUtils Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return blob;
    }

    @Deprecated
    public Map<String, Object> getImageMetadata(InputStream in) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    in);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    @Deprecated
    public Map<String, Object> getImageMetadata(File file) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    file);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public Map<String, Object> getImageMetadata(Blob blob) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    blob);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMetadata Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public String getImageMimeType(File file) {
        try {
            return getLibrarySelectorService().getMimeUtils().getImageMimeType(
                    file);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMime Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMime Class", e);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public String getImageMimeType(InputStream in) {
        try {
            return getLibrarySelectorService().getMimeUtils().getImageMimeType(
                    in);
        } catch (InstantiationException e) {
            log.error("Failed to instanciate ImageMime Class", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to instanciate ImageMime Class", e);
        } catch (ClientException e) {
            log.error(e, e);
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

    public ImageInfo getImageInfo(Blob blob) {
        ImageInfo imageInfo = null;
        File tmpFile = new File(System.getProperty("java.io.tmpdir"),
                blob.getFilename() != null ? blob.getFilename() : "tmp.tmp");
        try {
            blob.transferTo(tmpFile);
            imageInfo = ImageIdentifier.getInfo(tmpFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to get the ImageInfo for file"
                    + blob.getFilename(), e);
        } finally {
            tmpFile.delete();
        }
        return imageInfo;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIGURATION_PARAMETERS_EP.equals(extensionPoint)) {
            ImagingConfigurationDescriptor desc = (ImagingConfigurationDescriptor) contribution;
            configurationParameters.putAll(desc.getParameters());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIGURATION_PARAMETERS_EP.equals(extensionPoint)) {
            ImagingConfigurationDescriptor desc = (ImagingConfigurationDescriptor) contribution;
            for (String configuration : desc.getParameters().keySet()) {
                configurationParameters.remove(configuration);
            }
        }
    }

    public String getConfigurationValue(String configurationName) {
        return configurationParameters.get(configurationName);
    }

    public String getConfigurationValue(String configurationName,
            String defaultValue) {
        return configurationParameters.containsKey(configurationName) ? configurationParameters.get(configurationName)
                : defaultValue;
    }

    public void setConfigurationValue(String configurationName,
            String configurationValue) {
        configurationParameters.put(configurationName, configurationValue);
    }
}
