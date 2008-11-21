package org.nuxeo.ecm.webengine.gwt.client;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.gwt.client.ui.Images;
import org.nuxeo.ecm.webengine.gwt.client.ui.UIApplication;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class UI implements EntryPoint {

    private static Image EMPTY_IMAGE = null;    
    protected static Map<String, ImageBundle> images = new HashMap<String, ImageBundle>();
    
    public static Image getEmptyImage() {
        if (EMPTY_IMAGE == null) {
            EMPTY_IMAGE = getImages(Images.class).noimage().createImage();
        }
        return EMPTY_IMAGE;
    }
   

    @SuppressWarnings("unchecked")
    public static <T> T getImages(Class<T> bundleClass) {
         ImageBundle bundle = images.get(bundleClass.getName());
         if (bundle == null) {
             bundle = GWT.create(bundleClass);
             images.put(bundleClass.getName(), bundle);
         }
         return (T)bundle;
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
