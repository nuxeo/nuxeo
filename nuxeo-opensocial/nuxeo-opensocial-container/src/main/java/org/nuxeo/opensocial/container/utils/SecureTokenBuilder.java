package org.nuxeo.opensocial.container.utils;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.nuxeo.opensocial.container.component.PortalComponent;
import org.nuxeo.opensocial.container.component.PortalConfig;
/**
* @author Guillaume Cusnieux
*/
public class SecureTokenBuilder {

  public static String getSecureToken(String viewer, String owner, String gadgetUrl)
      throws Exception {

    PortalConfig config = PortalComponent.getInstance()
        .getConfig();

    String key = config.getKey();
    String container = config.getContainerName();
    String domain = config.getDomain();

    return getSecureToken(viewer, owner, gadgetUrl, key, container, domain);

  }

  private static String getSecureToken(String viewer, String owner,
      String gadgetUrl, String key, String container, String domain)
      throws Exception {
    BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(new BasicBlobCrypter(key.getBytes()),
        container, domain);
    st.setViewerId(viewer);
    st.setOwnerId(owner);
    st.setAppUrl(gadgetUrl);
    return Utf8UrlCoder.encode(st.encrypt());
  }

}
