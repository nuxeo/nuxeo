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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.jaxrs.usermanager;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.scim.server.mapper.AbstractMapper;
import org.nuxeo.scim.server.mapper.UserMapperFactory;

/**
 * Base class used for all WebObject associated to SCIM Domain model
 *
 * @author tiry
 * @since 7.4
 */

public abstract class BaseUMObject extends DefaultObject {

    protected static Log log = LogFactory.getLog(SCIMUserWebObject.class);

    protected UserManager um;

    protected AbstractMapper mapper;

    protected String baseUrl;

    // default to JSON
    protected MediaType fixeMediaType = null;

    public BaseUMObject() {
        super();
    }

    protected abstract String getPrefix();

    @Override
    protected void initialize(Object... args) {
        um = Framework.getService(UserManager.class);
        // build base url
        baseUrl = VirtualHostHelper.getBaseURL(WebEngine.getActiveContext().getRequest());
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        baseUrl = baseUrl + WebEngine.getActiveContext().getUrlPath();
        // remove end of url
        int idx = baseUrl.lastIndexOf(getPrefix());
        if (idx > 0) {
            baseUrl = baseUrl.substring(0, idx + getPrefix().length());
        }
        mapper = UserMapperFactory.getMapper(baseUrl);

        if (args != null && args.length > 0) {
            fixeMediaType = (MediaType) args[0];
        }
        if (fixeMediaType == null) {
            String accept = WebEngine.getActiveContext().getRequest().getHeader("Accept");
            if (accept != null && accept.toLowerCase().contains("application/xml")) {
                fixeMediaType = MediaType.APPLICATION_XML_TYPE;
            } else {
                fixeMediaType = MediaType.APPLICATION_JSON_TYPE;
            }
        }
    }

    protected void checkUpdateGuardPreconditions() {
        NuxeoPrincipal principal = getContext().getCoreSession().getPrincipal();
        if (!principal.isAdministrator()) {
            if ((!principal.isMemberOf("powerusers")) || !isAPowerUserEditableArtifact()) {

                throw new WebSecurityException("User is not allowed to edit users");
            }
        }
    }

    /**
     * Check that the current artifact is editable by a power user. Basically this means not an admin user or not an
     * admin group.
     *
     * @return
     */
    protected boolean isAPowerUserEditableArtifact() {
        return false;
    }

}
