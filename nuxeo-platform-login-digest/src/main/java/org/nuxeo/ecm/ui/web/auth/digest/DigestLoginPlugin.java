package org.nuxeo.ecm.ui.web.auth.digest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.BaseLoginModule;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class DigestLoginPlugin extends BaseLoginModule {

    private static final Log log = LogFactory.getLog(DigestLoginPlugin.class);

    protected static final String REALM = "realm";
    protected static final String HTTP_METHOD = "httpMethod";
    protected static final String URI = "uri";
    protected static final String QOP = "qop";
    protected static final String NONCE = "nonce";
    protected static final String NC = "nc";
    protected static final String CNONCE = "cnonce";
    protected static final String DIGEST_AUTH_DIRECTORY_NAME = "digest_auth";

    public Boolean initLoginModule() {
        return true;
    }

    public String validatedUserIdentity(UserIdentificationInfo userIdent) {
        try {
            String storedHA1 = getStoredHA1(userIdent.getUserName());

            if (StringUtils.isEmpty(storedHA1)) {
                log.warn("Digest authentication failed. Stored HA1 is empty");
                return null;
            }

        String generateDigest = generateDigest(
                    storedHA1,
                userIdent.getLoginParameters().get(HTTP_METHOD),
                userIdent.getLoginParameters().get(URI),
                userIdent.getLoginParameters().get(QOP),   // RFC 2617 extension
                userIdent.getLoginParameters().get(NONCE),
                userIdent.getLoginParameters().get(NC),    // RFC 2617 extension
                userIdent.getLoginParameters().get(CNONCE) // RFC 2617 extension
                );

        if(generateDigest.equals(userIdent.getPassword())){
            return userIdent.getUserName();
        } else {
                log.warn("Digest authentication failed for user:" + userIdent.getUserName() + " realm:"
                        + userIdent.getLoginParameters().get(REALM));
                return null;
            }
        } catch (Exception e) {
            log.error("Digest authentication failed", e);
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
    
    private String getStoredHA1(String username) throws Exception {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Directory directory = directoryService.getDirectory(DIGEST_AUTH_DIRECTORY_NAME);
        if (directory == null) {
            throw new IllegalArgumentException("Directory '" + DIGEST_AUTH_DIRECTORY_NAME + "' did not found.");
        }
        org.nuxeo.ecm.directory.Session directorySession = null;
        try {
            directorySession = directoryService.open(DIGEST_AUTH_DIRECTORY_NAME);
            String digestAuthDirectorySchema = directoryService.getDirectorySchema(DIGEST_AUTH_DIRECTORY_NAME);
            DocumentModel entry = directorySession.getEntry(username, true);
            if (entry == null) {
                return null;
            }
            return (String) entry.getProperty(digestAuthDirectorySchema, directorySession.getPasswordField());
        } finally {
            if (directorySession != null) {
                directorySession.close();
            }
        }
    }

}
