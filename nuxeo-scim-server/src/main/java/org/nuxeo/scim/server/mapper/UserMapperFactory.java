package org.nuxeo.scim.server.mapper;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;

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
