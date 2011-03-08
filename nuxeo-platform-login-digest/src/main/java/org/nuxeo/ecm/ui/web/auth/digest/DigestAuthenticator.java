package org.nuxeo.ecm.ui.web.auth.digest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class DigestAuthenticator implements NuxeoAuthenticationPlugin {

    protected static final String DEFAULT_REALMNAME = "NUXEO";
    protected static final Long DEFAULT_NONCE_VALIDITY_SECONDS = 1000L;

    protected static final String COMMA_SEPARATOR = ",";
    protected static final String EQUAL_SEPARATOR = "=";
    protected static final String QUOTE = "\"";
    protected static final String REALM_NAME_KEY = "RealmName";
    protected static final String BA_HEADER_NAME = "WWW-Authenticate";

    protected String realName;
    protected Long nonceValiditySeconds = DEFAULT_NONCE_VALIDITY_SECONDS;
    protected String accessKey = "key";

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse, String baseURL) {

        long expiryTime = System.currentTimeMillis() + (nonceValiditySeconds * 1000);
        String signature = DigestUtils.md5Hex(expiryTime + ":" + accessKey);
        String nonceValue = expiryTime + ":" + signature;
        String nonceValueBase64 = new String(
                org.apache.commons.codec.binary.Base64.encodeBase64(nonceValue.getBytes()));

        String authenticateHeader = "Digest realm=\"" + realName + "\", " + "qop=\"auth\", nonce=\""
            + nonceValueBase64 + "\"";

        try {
            httpResponse.addHeader(BA_HEADER_NAME, authenticateHeader);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String header = httpRequest.getHeader("Authorization");
        if (StringUtils.isEmpty(header) || !header.toLowerCase().startsWith("digest ")) {
            return null;
        }
        Map<String, String> headerMap = splitResponseParameters(header.substring(7));
        headerMap.put("httpMethod", httpRequest.getMethod());

        String nonce = headerMap.get("nonce");
        String nonceAsPlainText = new String(
                org.apache.commons.codec.binary.Base64.decodeBase64(nonce.getBytes()));
        String[] nonceTokens = nonceAsPlainText.split(":");
        long nonceExpiryTime = new Long(nonceTokens[0]).longValue();

        //@TODO: check expiry time and do something 

        String username = headerMap.get("username");
        String responseDigest = headerMap.get("response");
        UserIdentificationInfo userIdent = new UserIdentificationInfo(username, responseDigest);

        /*
        I have used this property to transfer response parameters to DigestLoginPlugin
        But loginParameters rewritten in NuxeoAuthenticationFilter common implementation
        @TODO: Fix this or find new way to transfer properties to LoginPlugin
        */
        userIdent.setLoginParameters(headerMap);
        return userIdent;

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        //@TODO: Use DIGEST authentication for WebDAV and WSS
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(REALM_NAME_KEY)) {
            realName = parameters.get(REALM_NAME_KEY);
        } else {
            realName = DEFAULT_REALMNAME;
        }
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    private Map<String, String> splitResponseParameters(String auth) {
        String[] array = auth.split(COMMA_SEPARATOR);

        if ((array == null) || (array.length == 0)) {
            return null;
        }

        Map<String, String> map = new HashMap<String, String>();
        for (String item : array) {

            item = StringUtils.replace(item, QUOTE, "");
            String[] parts = item.split(EQUAL_SEPARATOR);

            if (parts == null) {
                continue;
            }

            map.put(parts[0].trim(), item.substring(parts[0].length()+1));
        }

        return map;
    }

}
