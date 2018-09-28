/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionScheduled;
import org.nuxeo.ecm.restapi.server.jaxrs.blob.BlobObject;
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
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class ConvertAdapter extends DefaultAdapter {

    public static final String NAME = "convert";

    @GET
    public Blob convert(@QueryParam("converter") String converter, @QueryParam("type") String type,
            @QueryParam("format") String format, @Context UriInfo uriInfo) {
        BlobHolder bh = getBlobHolderToConvert();
        if (StringUtils.isNotBlank(converter)) {
            return convertWithConverter(bh, converter, uriInfo);
        } else if (StringUtils.isNotBlank(type)) {
            return convertWithMimeType(bh, type, uriInfo);
        } else if (StringUtils.isNotBlank(format)) {
            return convertWithFormat(bh, format, uriInfo);
        } else {
            throw new IllegalParameterException("No converter, type or format parameter specified");
        }
    }

    protected BlobHolder getBlobHolderToConvert() {
        Blob blob = getTarget().getAdapter(Blob.class);
        BlobHolder bh = null;
        if (blob == null) {
            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            if (doc != null) {
                bh = doc.getAdapter(BlobHolder.class);
                if (bh != null) {
                    blob = bh.getBlob();
                }
            }
        }
        if (blob == null) {
            throw new IllegalParameterException("No Blob found");
        }

        if (getTarget().isInstanceOf("blob")) {
            bh = ((BlobObject) getTarget()).getBlobHolder();
        }

        if (bh == null) {
            bh = new SimpleBlobHolder(blob);
        }
        return bh;
    }

    protected Blob convertWithConverter(BlobHolder bh, String converter, UriInfo uriInfo) {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        if (!conversionService.isConverterAvailable(converter).isAvailable()) {
            throw new IllegalParameterException(String.format("The '%s' converter is not available", converter));
        }
        Map<String, Serializable> parameters = computeConversionParameters(uriInfo);
        BlobHolder blobHolder = conversionService.convert(converter, bh, parameters);
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

    protected Blob convertWithMimeType(BlobHolder bh, String mimeType, UriInfo uriInfo) {
        Map<String, Serializable> parameters = computeConversionParameters(uriInfo);
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder blobHolder = conversionService.convertToMimeType(mimeType, bh, parameters);
        Blob conversionBlob = blobHolder.getBlob();
        if (conversionBlob == null) {
            throw new WebResourceNotFoundException(String.format("No converted Blob for '%s' mime type", mimeType));
        }
        return conversionBlob;
    }

    protected Blob convertWithFormat(BlobHolder bh, String format, UriInfo uriInfo) {
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimeType = mimetypeRegistry.getMimetypeFromExtension(format);
        return convertWithMimeType(bh, mimeType, uriInfo);
    }

    @POST
    public Object convert(@FormParam("converter") String converter, @FormParam("type") String type,
            @FormParam("format") String format, @FormParam("async") boolean async, @Context UriInfo uriInfo) {
        if (!async) {
            return convert(converter, type, format, uriInfo);
        }

        String conversionId;
        BlobHolder bh = getBlobHolderToConvert();
        Map<String, Serializable> parameters = computeConversionParameters(uriInfo);
        ConversionService conversionService = Framework.getService(ConversionService.class);
        if (StringUtils.isNotBlank(converter)) {
            conversionId = conversionService.scheduleConversion(converter, bh, parameters);
        } else if (StringUtils.isNotBlank(type)) {
            conversionId = conversionService.scheduleConversionToMimeType(type, bh, parameters);
        } else if (StringUtils.isNotBlank(format)) {
            MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
            String mimeType = mimetypeRegistry.getMimetypeFromExtension(format);
            conversionId = conversionService.scheduleConversionToMimeType(mimeType, bh, parameters);
        } else {
            throw new IllegalParameterException("No converter, type or format parameter specified");
        }

        String serverURL = StringUtils.stripEnd(ctx.getServerURL().toString(), "/");
        String pollingURL = String.format("%s%s/conversions/%s/poll", serverURL, ctx.getModulePath(), conversionId);
        String resultURL = String.format("%s%s/conversions/%s/result", serverURL, ctx.getModulePath(), conversionId);
        ConversionScheduled conversionScheduled = new ConversionScheduled(conversionId, pollingURL, resultURL);
        try {
            return Response.status(Response.Status.ACCEPTED)
                           .location(new URI(pollingURL))
                           .entity(conversionScheduled)
                           .build();
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }
}
