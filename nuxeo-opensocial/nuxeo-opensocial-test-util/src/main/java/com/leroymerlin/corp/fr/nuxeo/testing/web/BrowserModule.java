package com.leroymerlin.corp.fr.nuxeo.testing.web;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class BrowserModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BrowserConfig.class).to(StandardBrowserConfig.class)
        .in(Scopes.SINGLETON);
  }

}
