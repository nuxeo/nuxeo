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

package org.nuxeo.ecm.restapi.server.jaxrs.conversion;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConversionStatus;
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

        // conversion finished
        if (conversionStatus.status == ConversionStatus.Status.COMPLETED) {
            String serverURL = ctx.getServerURL().toString();
            if (serverURL.endsWith("/")) {
                serverURL = serverURL.substring(0, serverURL.length() - 1);
            }
            String url = String.format("%s%s/conversions/%s/result", serverURL, ctx.getModulePath(), id);
            try {
                return Response.seeOther(new URI(url)).build();
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        }

        return Response.ok(conversionStatus).build();
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
