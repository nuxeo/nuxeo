package org.nuxeo.ecm.platform.auth.saml.user;

import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;

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
        NuxeoPrincipal principal = Framework.getService(UserMapperService.class).getCreateOrUpdateNuxeoPrincipal(
                mapperName, userInfo);

        if (principal != null) {
            return principal.getName();
        }
        return null;
    }

}
