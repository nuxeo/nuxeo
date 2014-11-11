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

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguint</a>
 */
public class RotationPictureConverter implements Converter {

    private static final Log log = LogFactory.getLog(RotationPictureConverter.class);

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            ImagingService service = Framework.getService(ImagingService.class);
            List<Blob> results = new ArrayList<Blob>();
            List<Blob> sources = blobHolder.getBlobs();
            int angle = (Integer) parameters.get(ImagingConvertConstants.OPTION_ROTATE_ANGLE);
            for (Blob source : sources) {
                if (source != null) {
                    InputStream in = source.getStream();
                    if (in != null) {
                        InputStream result = service.rotate(in, angle);
                        // FIXME : local only
                        Blob blob = new FileBlob(result);
                        results.add(blob);
                    }
                }
            }
            return new SimpleCachableBlobHolder(results);
        } catch (Exception e) {
            throw new ConversionException("Rotation conversion has failed", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
