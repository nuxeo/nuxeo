package org.nuxeo.opensocial.shindig.gadgets;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.auth.UrlParameterAuthenticationHandler;

import com.google.inject.Inject;

public class NXAuthenticationHandler extends UrlParameterAuthenticationHandler {

    public static final String NX_COOKIE = "nuxeoCookie";

    public static final String JSESSIONID = "JSESSIONID";

    @Inject
    public NXAuthenticationHandler(SecurityTokenDecoder securityTokenDecoder) {
        super(securityTokenDecoder);
    }

    @Override
    public Map<String, String> getMappedParameters(
            final HttpServletRequest request) {
        Map<String, String> result = super.getMappedParameters(request);
        Cookie[] allCookies = request.getCookies();
        for (Cookie cookie : allCookies) {
            if (cookie.getName().equals(JSESSIONID)) {
                result.put(NX_COOKIE, JSESSIONID + "=" + cookie.getValue());
                break;
            }
        }
        return result;
    }
}
