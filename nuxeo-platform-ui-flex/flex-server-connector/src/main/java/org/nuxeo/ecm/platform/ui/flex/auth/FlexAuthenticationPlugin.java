package org.nuxeo.ecm.platform.ui.flex.auth;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.ui.web.auth.plugins.FormAuthenticator;

public class FlexAuthenticationPlugin extends FormAuthenticator {


    protected String loginPage = "login.swf";

    protected String usernameKey = "user_name";

    protected String passwordKey = "user_password";


    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        // Login Page is unauthenticated !
        List<String> prefix = new ArrayList<String>();
        prefix.add(loginPage);
        return prefix;
    }

}
