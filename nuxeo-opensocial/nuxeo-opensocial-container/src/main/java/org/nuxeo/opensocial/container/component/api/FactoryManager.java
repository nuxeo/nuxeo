package org.nuxeo.opensocial.container.component.api;

import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;

/**
 * Factory Manager is component for recover contribution factories
 *
 * @author Guillaume Cusnieux
 *
 */
public interface FactoryManager {

  public GadgetManager getGadgetFactory();;

  public ContainerManager getContainerFactory();

}
