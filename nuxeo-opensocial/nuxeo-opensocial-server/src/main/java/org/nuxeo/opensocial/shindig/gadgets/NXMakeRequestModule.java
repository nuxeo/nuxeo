package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.gadgets.servlet.MakeRequestHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class NXMakeRequestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MakeRequestHandler.class).to(NXMakeRequestHandler.class).in(
        Scopes.SINGLETON);

  }

}
