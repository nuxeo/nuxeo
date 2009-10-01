package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.gadgets.GadgetSpecFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class NXGadgetSpecFactoryModule extends AbstractModule {

  @Override
  protected void configure() {
    // TODO Auto-generated method stub
    bind(GadgetSpecFactory.class).to(NXGadgetSpecFactory.class)
    .in(Scopes.SINGLETON);


  }

}
