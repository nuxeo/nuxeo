/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.picture.convert;

import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.CONVERSION_FORMAT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_DEPTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_WIDTH;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public class ResizePictureConverter implements Converter {

    private static final Log log = LogFactory.getLog(ResizePictureConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            ImagingService service = Framework.getService(ImagingService.class);
            List<Blob> results = new ArrayList<Blob>();
            List<Blob> sources = blobHolder.getBlobs();
            Serializable h = parameters.get(OPTION_RESIZE_HEIGHT);
            int height = h == null ? 0 : (Integer) h;
            Serializable w = parameters.get(OPTION_RESIZE_WIDTH);
            int width = w == null ? 0 : (Integer) w;
            Serializable d = parameters.get(OPTION_RESIZE_DEPTH);
            int depth = d == null ? 0 : (Integer) d;
            // use the registered conversion format
            String format = (String) parameters.get(CONVERSION_FORMAT);

            for (Blob source : sources) {
                if (source != null) {
                    Blob result = service.resize(source, format, width, height,
                            depth);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }
            return new SimpleCachableBlobHolder(results);
        } catch (Exception e) {
            log.error(e);
            throw new ConversionException("Resize conversion has failed", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
