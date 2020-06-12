/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = RedirectResource.TYPE)
@Produces("text/html")
public class RedirectResource extends DefaultObject {

    /** @since 11.2 */
    public static final String TYPE = "redirectWO";

    protected String orgDistributionId = null;

    protected String targetDistributionId = null;

    @Override
    protected void initialize(Object... args) {
        orgDistributionId = (String) args[0];
        targetDistributionId = (String) args[1];
        targetDistributionId = targetDistributionId.replace(" ", "%20");
    }

    @GET
    @Produces("text/html")
    public Object get() {
        return newLocation(targetDistributionId, null);
    }

    @GET
    @Produces("text/html")
    @Path("/{subPath:.*}")
    public Object catchAll(@PathParam("subPath") String subPath) {
        return newLocation(targetDistributionId, subPath);
    }

    protected Response newLocation(String target, String subPath) {
        String path = getPrevious().getPath();
        String url = ctx.getServerURL().append(path).append("/").append(target).toString();
        if (subPath != null) {
            url = url + "/" + subPath;
        }
        return redirect(url);
    }
}
