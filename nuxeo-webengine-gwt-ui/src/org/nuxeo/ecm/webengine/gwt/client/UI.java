package org.nuxeo.ecm.webengine.gwt.client;

import org.nuxeo.ecm.webengine.gwt.client.ApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class UI implements EntryPoint {

    
    
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      ApplicationBundle bundle = GWT.create(MyBundle.class);
      bundle.start();
  }

}
