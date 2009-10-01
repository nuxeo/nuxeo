package org.nuxeo.opensocial.container.component;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PortalComponent extends DefaultComponent {

  private static final String PORTAL_COMPONENT_NAME = "org.nuxeo.opensocial.container.config";
  private static final String XP_CONFIG = "portalConfig";
  private PortalConfig config;

  public static PortalComponent getInstance() {
    return (PortalComponent) Framework.getRuntime()
        .getComponent(PORTAL_COMPONENT_NAME);
  }

  @Override
  public void registerContribution(Object contribution, String extensionPoint,
      ComponentInstance contributor) throws Exception {
    if (XP_CONFIG.equals(extensionPoint)) {
      PortalConfig contrib = (PortalConfig) contribution;
      this.config = contrib;
    }
  }

  @Override
  public void unregisterContribution(Object contribution,
      String extensionPoint, ComponentInstance contributor) throws Exception {
    super.unregisterContribution(contribution, extensionPoint, contributor);
  }

  public PortalConfig getConfig() {
    return config;
  }

}
