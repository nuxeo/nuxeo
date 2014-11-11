/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.webapp.seam;

import java.util.Set;

import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Restlet to trigger the reloading.
 * (can not be done directly from a Seam bean without messing up JSF scopes).
 *
 * @author tiry
 */
public class NuxeoSeamHotReloadRestTrigger extends BaseStatelessNuxeoRestlet {

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {

        StringBuffer sb = new StringBuffer();

        if (!SeamHotReloadHelper.isHotReloadEnabled()) {
            sb.append("This operation is not permited");
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
                if (reloadableComponents == null
                        || reloadableComponents.size() == 0) {
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
