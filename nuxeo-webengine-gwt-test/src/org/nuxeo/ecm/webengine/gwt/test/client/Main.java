package org.nuxeo.ecm.webengine.gwt.test.client;

import org.nuxeo.ecm.webengine.gwt.client.ApplicationBundle;
import org.nuxeo.ecm.webengine.gwt.client.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Main implements EntryPoint {

    protected static Images images = GWT.create(Images.class); 
    
    public static Images getImages() {
        return images;
    }
    
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      ApplicationBundle bundle = GWT.create(TestBundle.class);
      Framework.addContextListener(new ContextListener() {
          public void onContextEvent(int event) {
              if (event == ContextListener.INPUT) {
                  System.out.println(Framework.getContext().getInputObject());
                  UI.openEditor();
              }
          }
      });
      bundle.start("/redirect");
  }

}
