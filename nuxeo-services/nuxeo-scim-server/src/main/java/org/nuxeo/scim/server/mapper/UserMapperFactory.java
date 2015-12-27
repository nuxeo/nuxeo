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
