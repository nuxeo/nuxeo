/*
 * (C) Copyright 2007-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Max Stepanov
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingConfigurationDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.core.libraryselector.LibrarySelector;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ImagingComponent extends DefaultComponent implements
        ImagingService {

    private static final Log log = LogFactory.getLog(ImagingComponent.class);

    public static final String CONFIGURATION_PARAMETERS_EP = "configuration";

    protected Map<String, String> configurationParameters = new HashMap<String, String>();

    private LibrarySelector librarySelector;

    @Override
    public Blob crop(Blob blob, int x, int y, int width, int height) {
        try {
            return getLibrarySelectorService().getImageUtils().crop(blob, x, y,
                    width, height);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return blob;
    }

    @Override
    public Blob resize(Blob blob, String finalFormat, int width, int height,
            int depth) {
        try {
            return getLibrarySelectorService().getImageUtils().resize(blob,
                    finalFormat, width, height, depth);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return blob;
    }

    @Override
    public Blob rotate(Blob blob, int angle) {
        try {
            return getLibrarySelectorService().getImageUtils().rotate(blob,
                    angle);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return blob;
    }

    @Override
    public Map<String, Object> getImageMetadata(Blob blob) {
        try {
            return getLibrarySelectorService().getMetadataUtils().getImageMetadata(
                    blob);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getImageMimeType(File file) {
        try {
            MimetypeRegistry mimetypeRegistry = Framework.getLocalService(MimetypeRegistry.class);
            return mimetypeRegistry.getMimetypeFromFile(file);
        } catch (Exception e) {
            log.error("Unable to retrieve mime type", e);
        }
        return null;
    }

    @Override
    @Deprecated
    public String getImageMimeType(InputStream in) {
        try {
            return getLibrarySelectorService().getMimeUtils().getImageMimeType(
                    in);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

    @Override
    public ImageInfo getImageInfo(Blob blob) {
        ImageInfo imageInfo = null;
        File tmpFile = null;
        try {
            File file = BlobHelper.getFileFromBlob(blob);
            if (file == null) {
                tmpFile = File.createTempFile(
                        "nuxeoImageInfo",
                        blob.getFilename() != null ? "."
                                + FilenameUtils.getExtension(blob.getFilename())
                                : ".tmp");
                blob.transferTo(tmpFile);
                file = tmpFile;
            }
            imageInfo = ImageIdentifier.getInfo(file.getAbsolutePath());
        } catch (CommandNotAvailable e) {
            log.error("Failed to get ImageInfo for file " + blob.getFilename(),
                    e);
        } catch (IOException e) {
            log.error("Failed to tranfert file " + blob.getFilename(), e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
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

    @Override
    public String getConfigurationValue(String configurationName) {
        return configurationParameters.get(configurationName);
    }

    @Override
    public String getConfigurationValue(String configurationName,
            String defaultValue) {
        return configurationParameters.containsKey(configurationName) ? configurationParameters.get(configurationName)
                : defaultValue;
    }

    @Override
    public void setConfigurationValue(String configurationName,
            String configurationValue) {
        configurationParameters.put(configurationName, configurationValue);
    }
}
