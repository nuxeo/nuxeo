/*
 * (C) Copyright 2014-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.segment.io.web;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOUserFilter;

@WebObject(type = "segmentIOScriptResource")
@Path("/segmentIO")
@Produces("application/javascript")
public class SegmentIOScriptResource extends ModuleRoot {

    protected static final Log log = LogFactory.getLog(SegmentIOScriptResource.class);

    public static final String OPTED_OUT_CONDITION_PARAM = "optedOutCondition";

    @GET
    public Object anonymous() {
        return buildScript(null);
    }

    @GET
    @Path("user/{login}")
    public Object signed(@PathParam("login") String login) {
        return buildScript(login);
    }

    protected String buildJsonBlackListedLogins() {
        SegmentIO segmentIO = Framework.getService(SegmentIO.class);

        SegmentIOUserFilter filters = segmentIO.getUserFilters();
        StringBuffer json = new StringBuffer("[");
        if (filters != null) {
            if (!filters.isEnableAnonymous()) {
                String anonymous = filters.getAnonymousUserId();
                if (anonymous != null) {
                    json.append("'");
                    json.append(anonymous);
                    json.append("',");
                }
            }
            for (String login : filters.getBlackListedUsers()) {
                json.append("'");
                json.append(login);
                json.append("',");
            }
        }
        json.append("]");
        return json.toString();
    }

    protected Object buildScript(String login) {

        SegmentIO segmentIO = Framework.getService(SegmentIO.class);

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("writeKey", segmentIO.getWriteKey());
        ctx.put("debugMode", Boolean.toString(segmentIO.isDebugMode()));
        // get opted out condition
        String optedOutCondition = segmentIO.getGlobalParameters().get(OPTED_OUT_CONDITION_PARAM);
        if(StringUtils.isBlank(optedOutCondition)){
            optedOutCondition = "false";
        }
        ctx.put("optedOutCondition", optedOutCondition);

        NuxeoPrincipal principal = getContext().getPrincipal();

        if (principal != null) {
            if (login == null || principal.getName().equals(login)) {
                ctx.put("principal", principal);
            }
        }
        ctx.put("blackListedLogins", buildJsonBlackListedLogins());

        return getView("script").args(ctx);
    }

    @GET
    @Path("test")
    public Object test() {
        return getView("test");
    }

    @GET
    @Path("marketo/{email}")
    @Produces("text/plain")
    public String getMarketo(@PathParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        return MarketoHelper.getLeadHash(email);
    }
}
