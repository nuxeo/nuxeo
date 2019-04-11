/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.server.jaxrs.conversion;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConversionStatus;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionStatusWithResult;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.4
 */
@WebObject(type = "conversions")
public class ConversionRootObject extends DefaultObject {

    @GET
    @Path("{id}/poll")
    public Object doGetConversionStatus(@PathParam("id") String id) {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        ConversionStatus conversionStatus = conversionService.getConversionStatus(id);
        if (conversionStatus == null) {
            throw new WebResourceNotFoundException("No conversion job for id: " + id);
        }

        String serverURL = ctx.getServerURL().toString().replaceAll("/$", "");
        String resultURL = String.format("%s%s/conversions/%s/result", serverURL, ctx.getModulePath(), conversionStatus.id);
        ConversionStatusWithResult conversionStatusWithResult = new ConversionStatusWithResult(conversionStatus, resultURL);
        return Response.ok(conversionStatusWithResult).build();
    }

    @GET
    @Path("{id}/result")
    public Object doGetConversionResult(@PathParam("id") String id) {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = conversionService.getConversionResult(id, false);
        if (result == null || result.getBlob() == null) {
            throw new WebResourceNotFoundException("No conversion result for id: " + id);
        }

        return result.getBlob();
    }

}
