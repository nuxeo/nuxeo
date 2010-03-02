package org.nuxeo.opensocial.shindig.crypto;

import org.apache.shindig.auth.BlobCrypterSecurityTokenDecoder;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.config.ContainerConfig;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;

public class NXBlobCrypterSecurityTokenDecoder extends
        BlobCrypterSecurityTokenDecoder {

    @Inject
    public NXBlobCrypterSecurityTokenDecoder(ContainerConfig config) {
        super(config);
        try {
            for (String container : config.getContainers()) {
                OpenSocialService os = Framework.getService(OpenSocialService.class);
                String key = os.getKeyForContainer(container);

                if (key != null) {
                    BlobCrypter crypter = new BasicBlobCrypter(key.getBytes());
                    crypters.put(container, crypter);
                }

            }
        } catch (Exception e) {
            // Someone specified securityTokenKeyFile, but we couldn't load the
            // key. That merits killing
            // the server.
            throw new RuntimeException(e);
        }
    }

}
