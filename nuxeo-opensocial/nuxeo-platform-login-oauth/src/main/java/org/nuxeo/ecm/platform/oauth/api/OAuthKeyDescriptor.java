package org.nuxeo.ecm.platform.oauth.api;

import net.oauth.OAuthConsumer;
import net.oauth.signature.RSA_SHA1;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("keys")
public class OAuthKeyDescriptor {

  @XNode("@consumer")
  public String consumer;

  @XNode("public-key")
  public String publicKey;

  @XNode("private-key")
  public String privateKey;

  @XNode("certificate")
  public String certificate;

  public void setKeyForConsumer(OAuthConsumer consumer) {
    if("".equals(certificate)) {
      consumer.setProperty(RSA_SHA1.PUBLIC_KEY,publicKey);
    } else {
      consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, certificate);
    }
  }
}
