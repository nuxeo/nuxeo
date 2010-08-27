/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.management.web.detached.service;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.DetachedNuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import com.thoughtworks.xstream.XStream;

/**
*
* Serializes and exposes via a web service basic info for a given user
* using a DetachedNuxeoPrincipal
* @author Mariana Cedica
*/
@WebObject(type = "userService")
public class UserService extends DefaultObject {

    private static final Log log = LogFactory.getLog(UserManager.class);

    UserManager userManager;

    @Override
    protected void initialize(Object... args) {
        try {
            userManager = Framework.getService(UserManager.class);
        } catch (Exception e) {
            log.error("Unable to retreive the userManager", e);
        }
    }

    @POST
    @Path("userInfo")
    @Produces(MediaType.APPLICATION_XML)
    public String doPost() {
        try {
            NuxeoPrincipal principal = userManager.getPrincipal(ctx.getPrincipal().getName());
            DetachedNuxeoPrincipal detachedPrincipal = userManager.getDetachedNuxeoPrincipal(principal);
            return new XStream().toXML(detachedPrincipal);
        } catch (ClientException e) {
            log.error("Unable to serialize nuxeoPrincipal", e);
        }
        return null;
    }
}