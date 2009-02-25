/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.transform.compat;


import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.runtime.api.Framework;

public class PluginWrappingConverter extends BaseConverterWrapper implements Plugin {

    private static final long serialVersionUID = 1L;

    public PluginWrappingConverter(ConverterDescriptor descriptor) {
        super(descriptor);
    }

    public void setDefaultOptions(Map<String, Serializable> defaultOptions) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public List<TransformDocument> transform(Map<String, Serializable> options, TransformDocument... sources) throws Exception {
        Blob[] blobs = new Blob[sources.length];
        for (int i = 0; i < sources.length; i++) {
            blobs[i] = sources[i].getBlob();
        }
        return transform(options, blobs);
    }

    public List<TransformDocument> transform(Map<String, Serializable> options, Blob... blobs) throws Exception {
        List<Blob> blobList = Arrays.asList(blobs);
        BlobHolder bh = new SimpleBlobHolder(blobList);
        BlobHolder result = getConversionService().convert(
                descriptor.getConverterName(), bh, buildParameters(options));
        return TransformDocumensFactory.wrap(result);
    }

    public Map<String, Serializable> getDefaultOptions() {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        Map<String, String> parameters = descriptor.getParameters();
        if (parameters != null) {
            for (String k : parameters.keySet()) {
                options.put(k, parameters.get(k));
            }
        }
        return options;
    }

}
