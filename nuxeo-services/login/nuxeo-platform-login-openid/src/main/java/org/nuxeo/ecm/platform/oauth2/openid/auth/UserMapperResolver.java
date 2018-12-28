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

package org.nuxeo.ecm.platform.oauth2.openid.auth;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;
import org.nuxeo.usermapper.service.UserMapperService;

/**
 * UserResolver implementation that uses the {@link UserMapperService}
 *
 * @author tiry
 * @since 7.4
 */
public class UserMapperResolver extends UserResolver {

    protected String mapperName;

    public UserMapperResolver(OpenIDConnectProvider provider, String mapperName) {
        super(provider);
        this.mapperName = mapperName;
    }

    @Override
    public String findOrCreateNuxeoUser(OpenIDUserInfo userInfo) {
        UserMapper mapper = Framework.getService(UserMapperService.class).getMapper(mapperName);
        NuxeoPrincipal principal = Framework.doPrivileged(() -> mapper.getOrCreateAndUpdateNuxeoPrincipal(userInfo));
        if (principal != null) {
            return principal.getName();
        } else {
            return null;
        }
    }

    @Override
    protected String findNuxeoUser(OpenIDUserInfo userInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected DocumentModel updateUserInfo(DocumentModel user, OpenIDUserInfo userInfo) {
        throw new UnsupportedOperationException();
    }

}
