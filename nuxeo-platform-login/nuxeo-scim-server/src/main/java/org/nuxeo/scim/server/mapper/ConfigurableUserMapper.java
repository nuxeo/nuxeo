/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.scim.server.mapper;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;

import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;

/**
 * Mapper implementation that uses the {@link UserMapperService}
 *
 * @author tiry
 * @since 7.4
 */
public class ConfigurableUserMapper extends AbstractMapper {

    protected UserMapperService mapperService;

    protected static final String MAPPING_NAME = "scim";

    public ConfigurableUserMapper(String baseUrl) {
        super(baseUrl);
        mapperService = Framework.getService(UserMapperService.class);
    }

    @Override
    public UserResource getUserResourceFromNuxeoUser(DocumentModel userModel) throws Exception {

        UserResource userResource = new UserResource(CoreSchema.USER_DESCRIPTOR);

        String userId = (String) userModel.getProperty(um.getUserSchemaName(), um.getUserIdField());
        userResource.setUserName(userId);
        userResource.setId(userId);
        userResource.setExternalId(userId);
        NuxeoPrincipal principal = um.getPrincipal(userId);

        URI location = new URI(baseUrl + "/" + userId);
        Meta meta = new Meta(null, null, location, "1");
        userResource.setMeta(meta);

        return (UserResource) mapperService.wrapNuxeoPrincipal(MAPPING_NAME, principal, userResource, null);

    }

    @Override
    public DocumentModel createNuxeoUserFromUserResource(UserResource user) throws NuxeoException {

        NuxeoPrincipal principal = mapperService.getOrCreateAndUpdateNuxeoPrincipal(MAPPING_NAME, user, true, true,
                null);

        if (principal != null) {
            return um.getUserModel(principal.getName());
        }
        return null;
    }

    @Override
    public DocumentModel updateNuxeoUserFromUserResource(String uid, UserResource user) throws NuxeoException {

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("uid", uid);
        NuxeoPrincipal principal = mapperService.getOrCreateAndUpdateNuxeoPrincipal(MAPPING_NAME, user, false, true,
                params);
        if (principal != null) {
            return um.getUserModel(principal.getName());
        }
        return null;
    }

}
