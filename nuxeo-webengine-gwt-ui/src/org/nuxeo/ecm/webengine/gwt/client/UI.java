package org.nuxeo.ecm.webengine.gwt.client;

import org.nuxeo.ecm.webengine.gwt.client.ui.Images;
import org.nuxeo.ecm.webengine.gwt.client.ui.UIApplication;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class UI implements EntryPoint {

    private static Image EMPTY_IMAGE = null;    
    protected static Images imagesBundle = GWT.create(Images.class);
    protected static PopupPanel busy;
    
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
    
    public static void showError(Throwable t) {
        final PopupPanel panel = new PopupPanel(true, true);
        panel.add(new Label(t.getMessage()));
        panel.center();
//        panel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
//            public void setPosition(int offsetWidth, int offsetHeight) {
//                RootPanel root = RootPanel.get();
//                panel.setPopupPosition((root.getOffsetWidth() - offsetWidth) / 2, 
//                        (root.getOffsetHeight() - offsetHeight) / 2);
//            }
//        });        
    }
    
    public static void showBusy() {
        if (busy == null) {
            busy = new PopupPanel(false, true);
            //HorizontalPanel panel = new HorizontalPanel();            
            //panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
            //panel.add(new Image("images/loading.gif"));
            //panel.add(new HTML(<img src="images/loading.gif"));
//            panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
//            panel.add(new Label("Loading ..."));
            VerticalPanel panel = new VerticalPanel();
            panel.setSpacing(0);
            panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            panel.add(new Label("Loading ..."));
            panel.add(new Image("images/progress.gif"));
            busy.add(panel);
            busy.hide();
        }
        busy.center();
//        busy.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
//            public void setPosition(int offsetWidth, int offsetHeight) {
//                RootPanel root = RootPanel.get();
//                busy.setPopupPosition((root.getOffsetWidth() - offsetWidth) / 2, 
//                        (root.getOffsetHeight() - offsetHeight) / 2);
//            }
//        });        
    }
    
    public static void hideBusy() {
        if (busy == null) return;
        busy.hide();
    }
    
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      ApplicationBundle bundle = GWT.create(DefaultApplicationBundle.class);
      bundle.start();
  }

   
}
