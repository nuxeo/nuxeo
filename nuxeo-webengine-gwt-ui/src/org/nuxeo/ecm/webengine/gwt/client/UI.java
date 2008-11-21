package org.nuxeo.ecm.webengine.gwt.client;

import org.nuxeo.ecm.webengine.gwt.client.ui.Images;
import org.nuxeo.ecm.webengine.gwt.client.ui.UIApplication;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class UI implements EntryPoint {

    private static Image EMPTY_IMAGE = null;    
    protected static Images imagesBundle = GWT.create(Images.class);
    
    public static Images getImages() {
        return imagesBundle;
    }
    
    public static Image getEmptyImage() {
        if (EMPTY_IMAGE == null) {
            EMPTY_IMAGE = imagesBundle.noimage().createImage();
        }
        return EMPTY_IMAGE;
    }
   

    public static void openInEditor(Object input) {
        ((UIApplication)Framework.getApplication()).openInEditor(input);
    }

    public static void openInEditor(String name, Object input) {
        ((UIApplication)Framework.getApplication()).openInEditor(name, input);
    }

    public static void openEditor() {
        ((UIApplication)Framework.getApplication()).openEditor();
    }

    public static void openEditor(String name) {
        ((UIApplication)Framework.getApplication()).openEditor(name);
    }

    public static void showView(String name) {
        ((UIApplication)Framework.getApplication()).showView(name);
    }
    
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      ApplicationBundle bundle = GWT.create(DefaultApplicationBundle.class);
      bundle.start();
  }

   
}
