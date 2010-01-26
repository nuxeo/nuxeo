package org.nuxeo.opensocial.container.client.view;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.TabPanel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.WindowListenerAdapter;
import com.gwtext.client.widgets.form.Label;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.HorizontalLayout;
import com.gwtext.client.widgets.layout.VerticalLayout;

public class SearchForm {

    private final String docType;

    private static final Window window = new Window();

    public SearchForm(String docType) {
        this.docType = docType;
    }

    public void showForm() {
        Panel form = new Panel();
        form.setPaddings(10);
        form.setWidth("100%");
        form.setFrame(true);
        this.buildForm(form);

        window.clear();
        window.add(form);
        window.setWidth(400);
        window.setModal(true);
        window.show();
        window.syncSize();
        window.addListener(new WindowListenerAdapter() {
            @Override
            public void onClose(Panel panel) {
                super.onClose(panel);
            }
        });
    }

    private void buildForm(Panel form) {
        TabPanel tabPanel = new TabPanel();
        tabPanel.setActiveTab(0);
        Panel tabSearch = new Panel();

        tabSearch.setTitle("Rechercher");
        tabSearch.setAutoScroll(true);

        //Main Panel
        Panel panel = new Panel();
        panel.setLayout(new VerticalLayout(15));

        //Search Panel
        Panel searchPanel = new Panel();
        searchPanel.setLayout(new HorizontalLayout(15));
        
        Label lblSearch = new Label();
        lblSearch.setText("Rechercher");
        searchPanel.add(lblSearch);
        
        
        
        

        TextField searchField = new TextField();
        searchPanel.add(searchField);


    }



}
