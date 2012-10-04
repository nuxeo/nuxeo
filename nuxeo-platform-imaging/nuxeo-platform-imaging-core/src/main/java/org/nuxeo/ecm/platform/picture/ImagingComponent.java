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

import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.CONVERSION_FORMAT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.JPEG_CONVERSATION_FORMAT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPERATION_RESIZE;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_DEPTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_WIDTH;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingConfigurationDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureTemplate;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;
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
    public String getImageMimeType(Blob blob) {
        try {
            MimetypeRegistry mimetypeRegistry = Framework.getLocalService(MimetypeRegistry.class);
            return mimetypeRegistry.getMimetypeFromBlob(blob);
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

    @Override
    public PictureView computeViewFor(Blob blob, PictureTemplate pictureTemplate)
            throws IOException, ClientException {
        String mimeType = blob.getMimeType();
        if (mimeType == null) {
            blob.setMimeType(getImageMimeType(blob));
        }
        ImageInfo imageInfo = getImageInfo(blob);
        return computeViewFor(blob, pictureTemplate, imageInfo);
    }

    @Override
    public List<PictureView> computeViewsFor(Blob blob,
            List<PictureTemplate> pictureTemplates) throws IOException,
            ClientException {
        String mimeType = blob.getMimeType();
        if (mimeType == null) {
            blob.setMimeType(getImageMimeType(blob));
        }

        ImageInfo imageInfo = getImageInfo(blob);
        List<PictureView> views = new ArrayList<PictureView>();
        for (PictureTemplate pictureTemplate : pictureTemplates) {
            views.add(computeViewFor(blob, pictureTemplate, imageInfo));
        }
        return views;
    }

    protected PictureView computeViewFor(Blob blob,
            PictureTemplate pictureTemplate, ImageInfo imageInfo)
            throws IOException, ClientException {
        String title = pictureTemplate.getTitle();
        if ("Original".equals(title)) {
            return computeOriginalView(blob, pictureTemplate, imageInfo);
        } else if ("OriginalJpeg".equals(title)) {
            return computeOriginalJpegView(blob, pictureTemplate, imageInfo);
        } else {
            return computeView(blob, pictureTemplate, imageInfo);
        }
    }

    protected PictureView computeOriginalView(Blob blob,
            PictureTemplate pictureTemplate, ImageInfo imageInfo)
            throws IOException {
        String filename = blob.getFilename();
        String title = pictureTemplate.getTitle();
        String viewFilename = title + "_" + filename;
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(PictureView.FIELD_TITLE, pictureTemplate.getTitle());
        map.put(PictureView.FIELD_DESCRIPTION, pictureTemplate.getDescription());
        map.put(PictureView.FIELD_FILENAME, viewFilename);
        map.put(PictureView.FIELD_TAG, pictureTemplate.getTag());
        map.put(PictureView.FIELD_WIDTH, imageInfo.getWidth());
        map.put(PictureView.FIELD_HEIGHT, imageInfo.getHeight());

        Blob originalViewBlob = copyBlob(blob);
        originalViewBlob.setMimeType(blob.getMimeType());
        originalViewBlob.setFilename(viewFilename);
        map.put(PictureView.FIELD_CONTENT, (Serializable) originalViewBlob);
        return new PictureViewImpl(map);
    }

    protected Blob copyBlob(Blob blob) throws IOException {
        Blob persistedBlob = blob.persist();
        return new InputStreamBlob(persistedBlob.getStream());
    }

    protected PictureView computeOriginalJpegView(Blob blob,
            PictureTemplate pictureTemplate, ImageInfo imageInfo)
            throws ClientException, IOException {
        String filename = blob.getFilename();
        String title = pictureTemplate.getTitle();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(PictureView.FIELD_TITLE, pictureTemplate.getTitle());
        map.put(PictureView.FIELD_DESCRIPTION, pictureTemplate.getDescription());
        map.put(PictureView.FIELD_TAG, pictureTemplate.getTag());
        map.put(PictureView.FIELD_WIDTH, width);
        map.put(PictureView.FIELD_HEIGHT, height);
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_RESIZE_WIDTH, width);
        options.put(OPTION_RESIZE_HEIGHT, height);
        options.put(OPTION_RESIZE_DEPTH, imageInfo.getDepth());
        // always convert to jpeg
        options.put(CONVERSION_FORMAT, JPEG_CONVERSATION_FORMAT);
        BlobHolder bh = new SimpleBlobHolder(blob);
        ConversionService conversionService = Framework.getLocalService(ConversionService.class);
        bh = conversionService.convert(OPERATION_RESIZE, bh, options);

        Blob originalJpegBlob = bh.getBlob();
        if (originalJpegBlob == null) {
            originalJpegBlob = copyBlob(blob);
            originalJpegBlob.setMimeType(blob.getMimeType());
        }
        String viewFilename = computeViewFilename(filename,
                JPEG_CONVERSATION_FORMAT);
        viewFilename = title + "_" + viewFilename;
        map.put(PictureView.FIELD_FILENAME, viewFilename);
        originalJpegBlob.setFilename(viewFilename);
        map.put(PictureView.FIELD_CONTENT, (Serializable) originalJpegBlob);
        return new PictureViewImpl(map);
    }

    protected String computeViewFilename(String filename, String format) {
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename + "." + format;
        } else {
            return filename.substring(0, index + 1) + format;
        }
    }

    protected PictureView computeView(Blob blob,
            PictureTemplate pictureTemplate, ImageInfo imageInfo)
            throws ClientException, IOException {
        String filename = blob.getFilename();
        String title = pictureTemplate.getTitle();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(PictureView.FIELD_TITLE, pictureTemplate.getTitle());
        map.put(PictureView.FIELD_DESCRIPTION, pictureTemplate.getDescription());
        map.put(PictureView.FIELD_TAG, pictureTemplate.getTag());
        Point size = new Point(width, height);
        size = getSize(size, pictureTemplate.getMaxSize());
        map.put(PictureView.FIELD_WIDTH, size.x);
        map.put(PictureView.FIELD_HEIGHT, size.y);
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_RESIZE_WIDTH, size.x);
        options.put(OPTION_RESIZE_HEIGHT, size.y);
        options.put(OPTION_RESIZE_DEPTH, imageInfo.getDepth());
        // use the registered conversion format for 'Medium' and 'Thumbnail'
        // views
        String conversionFormat = getConfigurationValue(CONVERSION_FORMAT,
                JPEG_CONVERSATION_FORMAT);
        options.put(CONVERSION_FORMAT, conversionFormat);
        BlobHolder bh = new SimpleBlobHolder(blob);
        ConversionService conversionService = Framework.getLocalService(ConversionService.class);
        bh = conversionService.convert(OPERATION_RESIZE, bh, options);

        Blob viewBlob = bh.getBlob();
        if (viewBlob == null) {
            viewBlob = copyBlob(blob);
            viewBlob.setMimeType(blob.getMimeType());
        }
        String viewFilename = computeViewFilename(filename, conversionFormat);
        viewFilename = title + "_" + viewFilename;
        map.put(PictureView.FIELD_FILENAME, viewFilename);
        viewBlob.setFilename(viewFilename);
        map.put(PictureView.FIELD_CONTENT, (Serializable) viewBlob);
        return new PictureViewImpl(map);
    }

    protected static Point getSize(Point current, int max) {
        int x = current.x;
        int y = current.y;
        int newX;
        int newY;
        if (x > y) { // landscape
            newY = (y * max) / x;
            newX = max;
        } else { // portrait
            newX = (x * max) / y;
            newY = max;
        }
        if (newX > x || newY > y) {
            return current;
        }
        return new Point(newX, newY);
    }

    @Override
    public List<List<PictureView>> computeViewsFor(List<Blob> blobs,
            List<PictureTemplate> pictureTemplates) throws IOException,
            ClientException {
        List<List<PictureView>> allViews = new ArrayList<List<PictureView>>();
        for (Blob blob : blobs) {
            allViews.add(computeViewsFor(blob, pictureTemplates));
        }
        return allViews;
    }

}
