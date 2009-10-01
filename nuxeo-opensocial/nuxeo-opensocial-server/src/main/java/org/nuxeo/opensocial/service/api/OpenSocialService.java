package org.nuxeo.opensocial.service.api;

import java.net.Proxy;

import org.apache.shindig.gadgets.GadgetSpecFactory;

import com.google.inject.Injector;

public interface OpenSocialService {

    /**
     * Returns our own gadget Spec Factory
     * @return
     */
    GadgetSpecFactory getGadgetSpecFactory();


    /**
     * Specify the GUICE injector to user for the service
     * @param injector
     */
    void setInjector(Injector injector);


    /**
     * Get the symetric key for the given container
     * @param defaultContainer the container name
     * @return
     */
    String getKeyForContainer(String defaultContainer);


    /**
     * Returns the proxy settings if set
     * @return
     */
    Proxy getProxySettings();

}
