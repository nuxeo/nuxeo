package org.nuxeo.webengine.gwt.client;



import org.nuxeo.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.webengine.gwt.client.ui.Item;
import org.nuxeo.webengine.gwt.client.ui.login.LoginContainer;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Main implements EntryPoint {

    protected SimplePanel contentPanel;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        
//        new ApplicationWindowImpl().register();
//        new ViewContainerImpl().register();
//        new EditorContainerImpl().register();
        
        // --------- add some views
        // a dummy navigator view
        Button button1 = new Button("Show Tabs");
        Item item = new Item("navigator", button1);
        item.setTitle("Navigator");
        item.setIcon(new Image("http://code.google.com/webtoolkit/images/docs.gif"));
        button1.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                TabPanel w = new TabPanel();
                Application.getWindow().openInEditor(w);
            }
        });
        Application.registerExtension(ExtensionPoints.VIEWS_XP, item);                
        
        // another dummy view
        Button button2 = new Button("Open some text");
        item = new Item("dummy", button2);
        item.setTitle("Dummy");        
        button2.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                HTML w = new HTML("<h1>My Content</h1>Some html text!");                
                Application.getWindow().openInEditor(w);
                // switch to "login" view
                Application.getWindow().showView("login");                
            }
        });
        Application.registerExtension(ExtensionPoints.VIEWS_XP, item);
        
        // another dummy view
        item = new Item("hey", new HTML("hey!!!"));
        item.setTitle("Hey!");        
        Application.registerExtension(ExtensionPoints.VIEWS_XP, item);
        
        // a login view
        LoginContainer login = new LoginContainer();
        Application.addContextListener(login);
        item = new Item("login", login);
        item.setTitle("Login");
        Application.registerExtension(ExtensionPoints.VIEWS_XP, item);
        
        // add a default editor
//        Editor editor = new Editor("default", new SimplePanel()) {
//            @Override
//            public void refresh() {
//                Object input = Application.getContext().getInputObject();
//                ((SimplePanel)getWidget()).setWidget(new HTML(new Date().toString()));
//            }
//        };
//        Application.registerExtension(ExtensionPoints.EDITORS_XP, editor);
        
        Application.debugStart("http://localhost:8888/redirect/skin/wiki/css/wiki.css");
        
        GWT.log("Application started", null);
    }

}
