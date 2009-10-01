package org.nuxeo.opensocial.container.factory.utils;

import org.nuxeo.runtime.api.Framework;

public class ServerBase {

  private static final String LMPORTAL_DEPLOY_HOST = "lmportal.deploy.host";
  private static final String LMPORTAL_DEPLOY_PORT = "lmportal.deploy.port";
  private static final String HTTP = "http://";
  private static final String HTTP_SEPARATOR = ":";
  private static final String base = HTTP + Framework.getProperty(LMPORTAL_DEPLOY_HOST)
        + HTTP_SEPARATOR + Framework.getProperty(LMPORTAL_DEPLOY_PORT);


  public static String getBase() {
    return base;
  }
}
