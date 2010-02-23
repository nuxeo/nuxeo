package org.nuxeo.opensocial.shindig.crypto;

import java.util.Map;

import org.apache.shindig.auth.BasicSecurityTokenDecoder;
import org.apache.shindig.auth.BlobCrypterSecurityTokenDecoder;
import org.apache.shindig.auth.DefaultSecurityTokenDecoder;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.config.ContainerConfig;

import com.google.inject.Inject;

public class NXSecurityTokenDecoder implements SecurityTokenDecoder {

  private static final String SECURITY_TOKEN_TYPE = "gadgets.securityTokenType";

  private final SecurityTokenDecoder decoder;

  @Inject
  public NXSecurityTokenDecoder(ContainerConfig config) {
    String tokenType = config.getString(ContainerConfig.DEFAULT_CONTAINER, SECURITY_TOKEN_TYPE);
    if ("insecure".equals(tokenType)) {
      decoder = new BasicSecurityTokenDecoder();
    } else if ("secure".equals(tokenType)) {
      decoder = new NXBlobCrypterSecurityTokenDecoder(config);
    } else {
      throw new RuntimeException("Unknown security token type specified in " +
          ContainerConfig.DEFAULT_CONTAINER + " container configuration. " +
          SECURITY_TOKEN_TYPE + ": " + tokenType);
    }
  }

  public SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException {
    return decoder.createToken(tokenParameters);
  }

}
