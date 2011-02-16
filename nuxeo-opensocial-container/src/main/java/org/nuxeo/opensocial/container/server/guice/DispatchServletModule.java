package org.nuxeo.opensocial.container.server.guice;

import com.google.inject.servlet.ServletModule;

/**
 * @author Stéphane Fourrier
 */
public class DispatchServletModule extends ServletModule {

    @Override
    public void configureServlets() {
      serve("*/gwtContainer/dispatch").with(
                WebEngineDispatchServiceServlet.class);
    }

}
