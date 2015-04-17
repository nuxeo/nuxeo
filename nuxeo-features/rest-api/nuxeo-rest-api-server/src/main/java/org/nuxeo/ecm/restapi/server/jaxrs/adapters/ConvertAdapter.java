/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter allowing to convert a Blob using a named converter or a destination mime type.
 *
 * @since 7.3
 */
@WebAdapter(name = ConvertAdapter.NAME, type = "convertAdapter")
public class ConvertAdapter extends DefaultAdapter {

    public static final String NAME = "convert";

    @GET
    public Blob convert(@QueryParam("converter") String converter, @QueryParam("type") String type,
            @QueryParam("format") String format, @Context UriInfo uriInfo) {
        Blob blob = getTarget().getAdapter(Blob.class);
        if (blob == null) {
            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            if (doc != null) {
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (bh != null) {
                    blob = bh.getBlob();
                }
            }
        }
        if (blob == null) {
            throw new IllegalParameterException("No Blob found");
        }

        if (StringUtils.isNotBlank(converter)) {
            return convertWithConverter(blob, converter, uriInfo);
        } else if (StringUtils.isNotBlank(type)) {
            return convertWithMimeType(blob, type, uriInfo);
        } else if (StringUtils.isNotBlank(format)) {
            return convertWithFormat(blob, format, uriInfo);
        } else {
            throw new IllegalParameterException("No converter, type or format parameter specified");
        }
    }

    protected Blob convertWithConverter(Blob blob, String converter, UriInfo uriInfo) {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        if (!conversionService.isConverterAvailable(converter).isAvailable()) {
            throw new IllegalParameterException(String.format("The '%s' converter is not available", converter));
        }
        Map<String, Serializable> parameters = computeConversionParameters(uriInfo);
        BlobHolder blobHolder = conversionService.convert(converter, new SimpleBlobHolder(blob), parameters);
        Blob conversionBlob = blobHolder.getBlob();
        if (conversionBlob == null) {
            throw new WebResourceNotFoundException(String.format("No converted Blob using '%s' converter", converter));
        }
        return conversionBlob;
    }

    protected Map<String, Serializable> computeConversionParameters(UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Map<String, Serializable> parameters = new HashMap<>();
        for (String parameterKey : queryParams.keySet()) {
            parameters.put(parameterKey, queryParams.getFirst(parameterKey));
        }
        return parameters;
    }

    protected Blob convertWithMimeType(Blob blob, String mimeType, UriInfo uriInfo) {
        Map<String, Serializable> parameters = computeConversionParameters(uriInfo);
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder blobHolder = conversionService.convertToMimeType(mimeType, new SimpleBlobHolder(blob), parameters);
        Blob conversionBlob = blobHolder.getBlob();
        if (conversionBlob == null) {
            throw new WebResourceNotFoundException(String.format("No converted Blob for '%s' mime type", mimeType));
        }
        return conversionBlob;
    }

    protected Blob convertWithFormat(Blob blob, String format, UriInfo uriInfo) {
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimeType = mimetypeRegistry.getMimetypeFromExtension(format);
        return convertWithMimeType(blob, mimeType, uriInfo);
    }
}
