package org.nuxeo.opensocial.container.component;

import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class FactoryManagerImpl extends DefaultComponent implements
    FactoryManager {

  private static final String XP_CONFIG = "factoryConfig";
  private GadgetManager gadgetManager;
  private ContainerManager containerManager;

  @Override
  public void registerContribution(Object contribution, String extensionPoint,
      ComponentInstance contributor) throws Exception {
    if (XP_CONFIG.equals(extensionPoint)) {
      FactoryConfig contrib = (FactoryConfig) contribution;
      gadgetManager = (GadgetManager) Class.forName(contrib.getGadgetFactory())
          .newInstance();
      containerManager = (ContainerManager) Class.forName(
          contrib.getContainerFactory())
          .newInstance();
    }
  }

  @Override
  public void unregisterContribution(Object contribution,
      String extensionPoint, ComponentInstance contributor) throws Exception {
    super.unregisterContribution(contribution, extensionPoint, contributor);
  }

  public GadgetManager getGadgetFactory() {
    return gadgetManager;
  };

  public ContainerManager getContainerFactory() {
    return containerManager;
  };

}
