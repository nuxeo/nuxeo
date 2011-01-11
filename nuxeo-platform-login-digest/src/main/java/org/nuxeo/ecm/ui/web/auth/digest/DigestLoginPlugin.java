package org.nuxeo.ecm.ui.web.auth.digest;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.BaseLoginModule;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class DigestLoginPlugin extends BaseLoginModule {

    protected static final String REALM = "realm";
    protected static final String HTTP_METHOD = "httpMethod";
    protected static final String URI = "uri";
    protected static final String QOP = "qop";
    protected static final String NONCE = "nonce";
    protected static final String NC = "nc";
    protected static final String CNONCE = "cnonce";

    public Boolean initLoginModule() {
        return true;
    }

    public String validatedUserIdentity(UserIdentificationInfo userIdent) {

        //@TODO: get a1Md5 value from DB
        String a1Md5 = encodePassword(
                userIdent.getUserName(),
                userIdent.getLoginParameters().get(REALM),
                "Administrator"    //@TODO: This is hardcoded password value
        );

        String generateDigest = generateDigest(
                a1Md5,
                userIdent.getLoginParameters().get(HTTP_METHOD),
                userIdent.getLoginParameters().get(URI),
                userIdent.getLoginParameters().get(QOP),   // RFC 2617 extension
                userIdent.getLoginParameters().get(NONCE),
                userIdent.getLoginParameters().get(NC),    // RFC 2617 extension
                userIdent.getLoginParameters().get(CNONCE) // RFC 2617 extension
                );

        System.out.println("DIGEST: httpMethod: " + userIdent.getLoginParameters().get(HTTP_METHOD));
        System.out.println("DIGEST: uri: " + userIdent.getLoginParameters().get(URI));
        System.out.println("DIGEST: password: " + userIdent.getPassword());

        if(generateDigest.equals(userIdent.getPassword())){
            return userIdent.getUserName();
        } else {
            return null;
        }

    }

    
    public static String generateDigest(String a1Md5, String httpMethod, String uri, String qop, String nonce, String nc, String cnonce)
            throws IllegalArgumentException {

        String a2 = httpMethod + ":" + uri;
        String a2Md5 = DigestUtils.md5Hex(a2);

        String digest;
        if (qop == null) {
            digest = a1Md5 + ":" + nonce + ":" + a2Md5;
        } else if ("auth".equals(qop)) {
            digest = a1Md5 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + a2Md5;
        } else {
            throw new IllegalArgumentException("This method does not support a qop: '" + qop + "'");
        }

        return DigestUtils.md5Hex(digest);
    }

    public static String encodePassword(String username, String realm, String password) {
        String a1 = username + ":" + realm + ":" + password;
        return DigestUtils.md5Hex(a1);
    }
    
}
