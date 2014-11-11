package org.nuxeo.ecm.webengine.oauth2;

import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

public class WEOAuthConstants {

	public static final String CODE_URL_PARAMETER = "code";
	public static final String ERROR_URL_PARAMETER = "error";
	public static final String INSTALLED_APP_PARAMETER = "app";
	// UserId is mandatory
	public static final String INSTALLED_APP_USER_ID = "system";
	
	public static String getCallbackURL(String serviceProviderName, boolean isInstalledApp) {
        String url = BaseURL.getContextPath() + "/site/oauth2/" + serviceProviderName + "/callback";
        if (isInstalledApp) {
        	url += "?" + INSTALLED_APP_PARAMETER + "=true";
        }
        return url;
    }
	
	public static String getInstalledAppCallbackURL(String serviceProviderName) {
        return getCallbackURL(serviceProviderName, true);
    }
	
    public static String getDefaultCallbackURL(String serviceProviderName) {
        return getCallbackURL(serviceProviderName, false);
    }
}
