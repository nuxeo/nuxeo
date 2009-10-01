package org.nuxeo.opensocial.container.component.api;

import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;

/**
 * Factory Manager is component for recover contribution factories
 *
 * @author 10044826
 *
 */
public interface FactoryManager {

  public GadgetManager getGadgetFactory();;

  public ContainerManager getContainerFactory();

}
