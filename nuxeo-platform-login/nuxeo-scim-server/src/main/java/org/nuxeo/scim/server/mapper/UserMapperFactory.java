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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;

/**
 * factory to get the Mapper implementation
 *
 * @author tiry
 * @since 7.4
 */
public class UserMapperFactory {

    protected static AbstractMapper mapper = null;

    public static synchronized AbstractMapper getMapper(String baseUrl) {

        if (mapper == null) {
            UserMapperService ums = Framework.getService(UserMapperService.class);
            if (ums != null && ums.getAvailableMappings().contains(ConfigurableUserMapper.MAPPING_NAME)) {
                mapper = new ConfigurableUserMapper(baseUrl);
            } else {
                mapper = new StaticUserMapper(baseUrl);
            }
        }
        return mapper;
    }

}
