package org.nuxeo.ecm.platform.oauth2.openid.auth;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;

public class UserMapperResolver extends UserResolver {

    protected String mapperName;

    public UserMapperResolver(OpenIDConnectProvider provider, String mapperName) {
        super(provider);
    }

    @Override
    public String findOrCreateNuxeoUser(OpenIDUserInfo userInfo) {
        NuxeoPrincipal principal = Framework.getService(UserMapper.class).getOrCreateAndUpdateNuxeoPrincipal(userInfo);
        if (principal!=null) {
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
