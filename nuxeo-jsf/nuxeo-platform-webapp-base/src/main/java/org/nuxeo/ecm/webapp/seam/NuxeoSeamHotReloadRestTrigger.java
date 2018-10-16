/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webapp.seam;

import java.util.Set;

import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;

/**
 * Restlet to trigger the reloading. (can not be done directly from a Seam bean without messing up JSF scopes).
 *
 * @author tiry
 */
public class NuxeoSeamHotReloadRestTrigger extends BaseStatelessNuxeoRestlet {

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {

        StringBuffer sb = new StringBuffer();

        if (!SeamHotReloadHelper.isHotReloadEnabled()) {
            sb.append("This operation is not permitted");
        } else {

            long t0 = System.currentTimeMillis();
            Set<String> reloadedComponents = SeamHotReloadHelper.reloadSeamComponents(getHttpRequest(req));
            long t1 = System.currentTimeMillis();

            if (reloadedComponents != null) {
                sb.append("Reloaded ");
                sb.append(reloadedComponents.size());
                sb.append(" Seam components in ");
                sb.append(t1 - t0);
                sb.append("ms");
                sb.append("\n");

                for (String cn : reloadedComponents) {
                    sb.append("  ");
                    sb.append(cn);
                    sb.append("\n");
                }
            } else {
                Set<String> reloadableComponents = SeamHotReloadHelper.getHotDeployableComponents(getHttpRequest(req));
                if (reloadableComponents == null || reloadableComponents.size() == 0) {
                    sb.append("Nothing to reload");
                } else {
                    sb.append(reloadableComponents.size());
                    sb.append(" reloadable Seam Components\n");
                    sb.append("But nothing to reload (classes are up to date)");
                }
            }
        }
        res.setEntity(sb.toString(), MediaType.TEXT_PLAIN);
    }

}
