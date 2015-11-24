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
    }

    @Override
    public String findOrCreateNuxeoUser(OpenIDUserInfo userInfo) {
        NuxeoPrincipal principal = Framework.getService(UserMapper.class).getOrCreateAndUpdateNuxeoPrincipal(userInfo);
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
