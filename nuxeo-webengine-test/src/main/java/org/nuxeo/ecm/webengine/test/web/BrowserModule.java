package org.nuxeo.ecm.webengine.test.web;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class BrowserModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BrowserConfig.class).to(StandardBrowserConfig.class)
        .in(Scopes.SINGLETON);
  }

}
