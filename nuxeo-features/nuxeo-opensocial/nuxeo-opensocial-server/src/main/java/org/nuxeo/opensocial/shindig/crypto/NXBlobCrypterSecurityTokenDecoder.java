package org.nuxeo.opensocial.shindig.crypto;

import java.io.FileReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.BlobCrypterSecurityTokenDecoder;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerIndex;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;

public class NXBlobCrypterSecurityTokenDecoder extends
        BlobCrypterSecurityTokenDecoder {

    private static final Log log = LogFactory.getLog(NXBlobCrypterSecurityTokenDecoder.class);

    @Inject
    public NXBlobCrypterSecurityTokenDecoder(ContainerConfig config,
            OAuthStore store) {
        super(config);
        try {
            OpenSocialService os = Framework.getService(OpenSocialService.class);
            for (String container : config.getContainers()) {
                String key = IOUtils.toString(new FileReader(
                        os.getSigningStateKeyFile()));
                if (key != null) {
                    BlobCrypter crypter = new BasicBlobCrypter(key.getBytes());
                    crypters.put(container, crypter);
                } else {
                    log.error("Should not be able to run any opensocial instance "
                            + "without a signing state key!");
                }

                /*
                 * It's unclear that this is really the right place to do this
                 */
                if (!(store instanceof BasicOAuthStore)) {
                    log.warn("We expected to be able to use a BasicOAuthStore "
                            + "to configure OAuth services!");
                } else {
                    for (OAuthServiceDescriptor descrptor : os.getOAuthServices()) {
                        BasicOAuthStore oauthStore = (BasicOAuthStore) store;
                        BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
                        index.setGadgetUri(descrptor.getGadgetUrl());
                        index.setServiceName(descrptor.getServiceName());
                        String oauthKey = IOUtils.toString(new FileReader(
                                os.getOAuthPrivateKeyFile()));
                        if (!StringUtils.isEmpty(descrptor.getConsumerSecret())) {
                            oauthKey = descrptor.getConsumerSecret();
                        }
                        BasicOAuthStoreConsumerKeyAndSecret keyAndSecret = new BasicOAuthStoreConsumerKeyAndSecret(
                                descrptor.getConsumerKey(), oauthKey,
                                KeyType.RSA_PRIVATE,
                                os.getOAuthPrivateKeyName(),
                                os.getOAuthCallbackUrl());
                        oauthStore.setConsumerKeyAndSecret(index, keyAndSecret);
                    }
                }
            }
        } catch (Exception e) {
            // Someone specified securityTokenKeyFile, but we couldn't load the
            // key. That merits killing
            // the server.
            throw new RuntimeException(e);
        }
    }

    // @Override
    // public SecurityToken createToken(Map<String, String> tokenParameters)
    // throws SecurityTokenException {
    // SecurityToken anon = super.createToken(tokenParameters);
    // if (anon.isAnonymous()) {
    // if (tokenParameters.get(NXAuthenticationHandler.NX_COOKIE) != null) {
    // return new NxSecurityToken(anon.getViewerId(),
    // anon.getOwnerId(), null,
    // tokenParameters.get(NXAuthenticationHandler.NX_COOKIE));
    // } else {
    // return anon;
    // }
    // }
    // BlobCrypterSecurityToken token = (BlobCrypterSecurityToken) anon;
    // // convert to nuxeo token
    // NxSecurityToken tokenResult = new NxSecurityToken(token.getViewerId(),
    // token.getOwnerId(), null,
    // tokenParameters.get(NXAuthenticationHandler.NX_COOKIE));
    // return tokenResult;
    // }
}
