package org.nuxeo.ecm.platform.gwt.client.ui;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.model.Context;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

/**
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UI {

    protected static Context ctx = new Context();    
    protected static ProgressTimer progressTimer = new ProgressTimer();
       

    
    public static Context getContext() {
        return ctx;
    }

    public static void openInEditor(Object input) {
        ((UIApplication)Framework.getApplication()).openInEditor(input);
    }

    public static View<?> getView(String name) {
        return ((UIApplication)Framework.getApplication()).getView(name); 
    }
    
    public static void showView(String name) {
        ((UIApplication)Framework.getApplication()).showView(name);
    }
    
    public static void showError(Throwable t) {
        Window.alert("Error: "+t.getMessage());
    }
    
    public static void showBusy() {
        progressTimer.start(100);
    }
    
    public static void hideBusy() {
        progressTimer.cancel();
    }

    public static void openDocument(String ref) {
        History.newItem("doc_"+ref);
    }

}
