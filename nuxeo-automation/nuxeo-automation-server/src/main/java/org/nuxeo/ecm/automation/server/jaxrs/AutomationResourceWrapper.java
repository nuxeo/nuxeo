/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.jaxrs.io.operations.AutomationInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * AutomationResourceWrapper.
 *
 * @since 5.7.3
 */
@Path("automation")
public class AutomationResourceWrapper {

    @Path("/")
    public Object getAutomationEndPoint() throws Exception {
        return new AutomationResource();
    }

    @GET
    public AutomationInfo getAutomationInfo() throws OperationException {
        return new AutomationInfo(Framework.getLocalService(AutomationService.class));
    }

}
