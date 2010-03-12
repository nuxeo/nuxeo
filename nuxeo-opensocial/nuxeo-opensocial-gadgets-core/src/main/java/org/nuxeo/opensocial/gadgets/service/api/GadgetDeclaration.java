package org.nuxeo.opensocial.gadgets.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is how code outside of the gadget implementation sees the gadget. The
 * implementation can be an internal or external gadget. Callers should not
 * depend on particular implementation strategies since they may vary quite
 * widely.
 * 
 * @author Ian Smith<iansmith@nuxecloud.com>
 * 
 */
public interface GadgetDeclaration {

    String getName();

    boolean getDisabled();

    String getCategory();

    String getIconUrl();

    InputStream getResourceAsStream(String resourcePath) throws IOException;

    URL getGadgetDefinition() throws MalformedURLException;
}
