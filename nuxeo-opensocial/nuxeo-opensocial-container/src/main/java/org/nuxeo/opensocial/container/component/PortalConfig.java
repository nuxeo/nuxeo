package org.nuxeo.opensocial.container.component;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("portalConfig")
public class PortalConfig {

  @XNode("containerName")
  private String containerName;

  @XNode("domain")
  private String domain;

  @XNode("key")
  private String key;

  public String getDomain() {
    return domain;
  }

  public String getContainerName() {
    return containerName;
  }

  public String getKey() {
    return key;
  }

}
