/*
 * (C) Copyright 2015-2018 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.auth.saml.user;

import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;

/**
 * UserResolver implementation that uses the {@link UserMapperService}
 *
 * @author tiry
 * @since 7.4
 */
public class UserMapperBasedResolver implements UserResolver {

    protected static final String USER_RESOLVER_MAPPING = "userResolverMapping";
    
    protected static final String USER_RESOLVER_CREATE_IF_NEEDED = "userResolverCreateIfNeeded";
    
    protected static final String USER_RESOLVER_UPDATE = "userResolverUpdate";

    protected static final String DEFAULT_USER_MAPPER_CONFIG = "saml";

    protected String mapperName = DEFAULT_USER_MAPPER_CONFIG;
    
    protected boolean createIfNeeded = true;

    protected boolean update = true;

    @Override
    public void init(Map<String, String> parameters) {
        if (parameters.containsKey(USER_RESOLVER_MAPPING)) {
            mapperName = parameters.get(USER_RESOLVER_MAPPING);
        }
        if (parameters.containsKey(USER_RESOLVER_CREATE_IF_NEEDED)) {
        	createIfNeeded = Boolean.getBoolean(parameters.get(USER_RESOLVER_CREATE_IF_NEEDED));
        }
        if (parameters.containsKey(USER_RESOLVER_UPDATE)) {
        	update = Boolean.getBoolean(parameters.get(USER_RESOLVER_UPDATE));
        }
    }

    @Override
    public String findOrCreateNuxeoUser(SAMLCredential userInfo) {
        NuxeoPrincipal principal = Framework.getService(UserMapperService.class).getOrCreateAndUpdateNuxeoPrincipal(
                mapperName, userInfo, createIfNeeded, update, null);
        
        if (principal != null) {
            return principal.getName();
        }
        return null;
    }

}
