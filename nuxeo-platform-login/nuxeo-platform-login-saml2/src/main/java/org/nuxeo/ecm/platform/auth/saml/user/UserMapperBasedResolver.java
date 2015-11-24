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

    protected static final String DEFAULT_USER_MAPPER_CONFIG = "saml";

    protected String mapperName = DEFAULT_USER_MAPPER_CONFIG;

    @Override
    public void init(Map<String, String> parameters) {
        if (parameters.containsKey(USER_RESOLVER_MAPPING)) {
            mapperName = parameters.get(USER_RESOLVER_MAPPING);
        }
    }

    @Override
    public String findOrCreateNuxeoUser(SAMLCredential userInfo) {
        NuxeoPrincipal principal = Framework.getService(UserMapperService.class).getOrCreateAndUpdateNuxeoPrincipal(
                mapperName, userInfo);

        if (principal != null) {
            return principal.getName();
        }
        return null;
    }

}
