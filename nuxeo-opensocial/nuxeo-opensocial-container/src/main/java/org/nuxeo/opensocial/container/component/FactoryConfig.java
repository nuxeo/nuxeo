package org.nuxeo.opensocial.container.component;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("factoryConfig")
public class FactoryConfig {

  @XNode("gadget")
  private String gadgetFactory;

  @XNode("container")
  private String containerFactory;

  public String getContainerFactory() {
    return containerFactory;
  }

  public String getGadgetFactory() {
    return gadgetFactory;
  }

}
