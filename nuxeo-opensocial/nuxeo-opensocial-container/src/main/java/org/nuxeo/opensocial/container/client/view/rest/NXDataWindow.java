package org.nuxeo.opensocial.container.client.view.rest;

import com.google.gwt.json.client.JSONArray;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.WindowListenerAdapter;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;

public class NXDataWindow {

  private Window window = new Window();
  private TextField field;

  public NXDataWindow(JSONArray array, final Window parent, TextField field) {
    this.field = field;
    window.clear();
    Panel p = new Panel();
    p.setBorder(false);
    p.setLayout(new FitLayout());
    Panel borderPanel = new Panel();
    borderPanel.setLayout(new BorderLayout());
    Panel westPanel = new Panel();
    westPanel.setWidth(200);
    westPanel.setLayout(new FitLayout());
    Panel settingsPanel = new Panel();
    settingsPanel.setBorder(false);
    settingsPanel.setPaddings(5);
    westPanel.add(settingsPanel);
    BorderLayoutData westData = new BorderLayoutData(RegionPosition.WEST);
    westData.setSplit(true);
    borderPanel.add(westPanel, westData);
    Panel centerPanel = new NXDataView(array, settingsPanel, this);
    BorderLayoutData centerData = new BorderLayoutData(RegionPosition.CENTER);
    borderPanel.add(centerPanel, centerData);
    p.add(borderPanel);
    p.setWidth(750);
    p.setHeight(500);
    window.add(p);
    window.setModal(true);
    parent.hide();
    window.show();
    window.syncSize();
    window.addListener(new WindowListenerAdapter() {
      @Override
      public void onClose(Panel panel) {
        parent.show();
        super.onClose(panel);
      }
    });
  }

  public void setValue(String fieldValue) {
    field.setValue(fieldValue);
    window.close();
  }

}