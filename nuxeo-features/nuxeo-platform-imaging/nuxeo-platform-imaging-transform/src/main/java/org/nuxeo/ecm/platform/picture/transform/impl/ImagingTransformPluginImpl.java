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
package org.nuxeo.ecm.platform.picture.transform.impl;

import static org.nuxeo.ecm.platform.picture.transform.api.ImagingTransformConstants.*;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Max Stepanov
 *
 */
public class ImagingTransformPluginImpl extends AbstractPlugin {

    private static final long serialVersionUID = 8917837724437954053L;

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        List<TransformDocument> results = super.transform(options, sources);

        String operation = (String) options.get(OPTION_OPERATION);
        ImagingService service = Framework.getService(ImagingService.class);
        if (OPERATION_RESIZE.equals(operation)) {
            int width = Integer.parseInt((String) options.get(OPTION_RESIZE_WIDTH));
            int height = Integer.parseInt((String) options.get(OPTION_RESIZE_HEIGHT));
            for (TransformDocument source : sources) {
                if (source != null) {
                    InputStream in = source.getBlob().getStream();
                    if (in != null) {
                        InputStream result = service.resize(in, width, height);
                        FileBlob blob = new FileBlob(result);
                        results.add(new TransformDocumentImpl(blob,
                                getDestinationMimeType()));
                    }
                }
            }
        } else if (OPERATION_CROP.equals(operation)) {
            int width = Integer.parseInt((String) options.get(OPTION_RESIZE_WIDTH));
            int height = Integer.parseInt((String) options.get(OPTION_RESIZE_HEIGHT));
            int x = Integer.parseInt((String) options.get(OPTION_CROP_X));
            int y = Integer.parseInt((String) options.get(OPTION_CROP_Y));
            for (TransformDocument source : sources) {
                if (source != null) {
                    InputStream in = source.getBlob().getStream();
                    if (in != null) {
                        InputStream result = service.crop(in, x, y, width, height);
                        // FIXME : local only
                        FileBlob blob = new FileBlob(result);
                        results.add(new TransformDocumentImpl(blob,
                                getDestinationMimeType()));
                    }
                }
            }
            }else if (OPERATION_ROTATE.equals(operation)) {
                int angle = Integer.parseInt((String) options.get(OPTION_ROTATE_ANGLE));
                for (TransformDocument source : sources) {
                    if (source != null) {
                        InputStream in = source.getBlob().getStream();
                        if (in != null) {
                            InputStream result = service.rotate(in, angle);
                            // FIXME : local only
                            FileBlob blob = new FileBlob(result);
                            results.add(new TransformDocumentImpl(blob,
                                    getDestinationMimeType()));
                        }
                    }
                }
        } else {
            throw new IllegalArgumentException("Unsupported operation <"
                    + operation + '>');
        }
        return results;
    }

}
