package org.nuxeo.opensocial.shindig.crypto;

import java.util.Map;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.CharsetUtil;

import com.google.common.collect.Maps;

public class BlobCrypterSecurityTokenDecoder implements
        SecurityTokenDecoder {

    public static final String SIGNED_FETCH_DOMAIN = "gadgets.signedFetchDomain";
    private BlobCrypter crypter;


    /**
     * Keys are container ids, values are domains used for signed fetch.
     */
    private Map<String, String> domains = Maps.newHashMap();

    public BlobCrypterSecurityTokenDecoder(ContainerConfig config, String key) {
        byte[] keyBytes = CharsetUtil.getUtf8Bytes(key.trim());
        crypter = new BasicBlobCrypter(keyBytes);
        for (String container : config.getContainers()) {

            String domain = config.get(container, SIGNED_FETCH_DOMAIN);
            domains.put(container, domain);
          }
    }

    public SecurityToken createToken(Map<String, String> tokenParameters)
            throws SecurityTokenException {
        String token = tokenParameters.get(SecurityTokenDecoder.SECURITY_TOKEN_NAME);
        if (token == null || token.trim().length() == 0) {
          // No token is present, assume anonymous access
          return new AnonymousSecurityToken();
        }
        String[] fields = token.split(":");
        if (fields.length != 2) {
          throw new SecurityTokenException("Invalid security token " + token);
        }
        String container = fields[0];

        if (crypter == null) {
          throw new SecurityTokenException("Unknown container " + token);
        }
        String domain = domains.get(container);
        String crypted = fields[1];
        try {
          return BlobCrypterSecurityToken.decrypt(crypter, container, domain, crypted);
        } catch (BlobCrypterException e) {
          throw new SecurityTokenException(e);
        }
    }

}
