package org.nuxeo.opensocial.service.test;

import org.apache.shindig.gadgets.http.HttpFetcher;

import com.google.inject.AbstractModule;

public class GadgetSpecFactoryTestModule extends AbstractModule {

  @Override
  protected void configure() {
    // TODO Auto-generated method stub
    bind(HttpFetcher.class).to(RecordingFetcher.class);
  }

}
