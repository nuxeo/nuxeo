package org.nuxeo.opensocial.container.client.view.rest;

import org.nuxeo.opensocial.container.client.ContainerConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.XTemplate;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.MemoryProxy;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.DataView;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.DataViewListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;

public class NXDataView extends Panel {

  private final static ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

  private static final String SHORT_DESCRIPTION = "shortDescription";
  private static final String SHORT_TITLE = "shortTitle";
  private static final String ID = "id";
  private static final String URL = "url";
  private static final String CREATOR = "creator";
  private static final String DESCRIPTION = "description";
  private static final String TITLE = "title";

  private Panel settingsPanel;

  public NXDataView(JSONArray jsonArray, Panel settingsPanel,
      final NXDataWindow nxDataWindow) {
    this.setBorder(true);
    this.settingsPanel = settingsPanel;
    Object[][] datas = getDatas(jsonArray);

    MemoryProxy dataProxy = new MemoryProxy(datas);
    RecordDef recordDef = new RecordDef(new FieldDef[] {
        new StringFieldDef(TITLE), new StringFieldDef(DESCRIPTION),
        new StringFieldDef(CREATOR), new StringFieldDef(URL),
        new StringFieldDef(ID) });

    ArrayReader reader = new ArrayReader(recordDef);
    Store store = new Store(dataProxy, reader, true);

    store.load();

    XTemplate template = new XTemplate(
        new String[] {
            "<tpl for='.'>",
            "<div class='thumb-wrap'>",
            "<div class='thumb'><img src='{url}' ext:qtip='{description}'></div>",
            "<label class='x-template' ext:qtip='{title}'>{shortTitle}</label></div>",
            "</tpl>", "<div class='x-clear'></div>" });

    Panel inner = new Panel();
    inner.setId("images-view");
    inner.setFrame(true);
    inner.setLayout(new FitLayout());
    inner.setHeight(450);
    inner.setAutoScroll(true);
    inner.setHeader(false);

    final DataView dataView = new DataView("div.thumb-wrap") {
      public void prepareData(Data data) {
        data.setProperty(SHORT_TITLE, Format.ellipsis(data.getProperty(TITLE),
            15));
        data.setProperty(SHORT_DESCRIPTION, Format.ellipsis(
            data.getProperty(DESCRIPTION) + " ...", 500));
      }
    };
    dataView.setWidth(535);

    dataView.addListener(new DataViewListenerAdapter() {
      public boolean doBeforeClick(DataView source, int index, Element node,
          EventObject e) {
        return true;
      }

      public boolean doBeforeSelect(DataView source, Element node,
          Element[] selections) {
        return super.doBeforeSelect(source, node, selections);
      }

      public void onClick(DataView source, int index, Element node,
          EventObject e) {
        super.onClick(source, index, node, e);
      }

      public void onContainerClick(DataView source, EventObject e) {
        super.onContainerClick(source, e);
      }

      public void onContextMenu(DataView source, int index, Element node,
          EventObject e) {
        super.onContextMenu(source, index, node, e);
      }

      public void onDblClick(DataView source, int index, Element node,
          EventObject e) {
        nxDataWindow.setValue(source.getSelectedRecords()[0].getAsString(ID));
        super.onDblClick(source, index, node, e);
      }

      public void onSelectionChange(DataView view, Element[] selections) {
        super.onSelectionChange(view, selections);
      }
    });

    dataView.setStore(store);
    dataView.setTpl(template);
    dataView.setAutoHeight(true);
    dataView.setOverCls("x-view-over");
    dataView.setSingleSelect(true);
    Timer t = new Timer() {

      @Override
      public void run() {
        dataView.select(0);
        updateSettingsPanel(dataView.getSelectedRecords());
      }

    };

    t.schedule(500);

    inner.add(dataView);

    Panel footer = new Panel();
    footer.setId("footer-view");
    footer.setFrame(true);
    footer.setLayout(new FitLayout());
    footer.setHeight(50);
    Button b = new Button();
    b.setText(CONSTANTS.add());
    b.addClickListener(new ClickListener() {

      public void onClick(Widget arg0) {
        nxDataWindow.setValue(dataView.getSelectedRecords()[0].getAsString(ID));
      }
    });
    footer.add(b);

    this.add(inner);
    this.add(footer);
  }

  private Object[][] getDatas(JSONArray jsonArray) {
    Object[][] datas = new Object[jsonArray.size()][4];
    for (int i = 0; i < jsonArray.size(); i++) {
      JSONValue val = jsonArray.get(i);
      JSONObject o = val.isObject();
      datas[i][0] = o.get(TITLE)
          .isString()
          .stringValue();

      datas[i][1] = o.get(DESCRIPTION)
          .isString()
          .stringValue();
      datas[i][2] = o.get(CREATOR)
          .isString()
          .stringValue();
      datas[i][3] = "http://localhost:8080/nuxeo/site/gadgets/picturebook/picturebook_icon.png";

      datas[i][4] = o.get(ID)
          .isString()
          .stringValue();
    }

    return datas;
  }

  private void updateSettingsPanel(Record[] records) {
    final Record record = records[0];
    // TODO: just for picturebook ??
    NXRestAPI.queryCurrentDocChildren(record.getAsString(ID),
        new RequestCallback() {

          public void onResponseReceived(Request arg0, Response rep) {

            JSONArray array = JSONParser.parse(rep.getText())
                .isObject()
                .get("data")
                .isArray();

            StringBuilder sb = new StringBuilder(
                "<div style='width:190px;'><div><label class='x-view-title'>");
            sb.append(record.getAsString(TITLE));
            sb.append("</label><br/></div>");
            if (array.size() > 0) {
              sb.append("<div class='x-view-image'><img src=");
              sb.append(NXRestAPI.getImageUrl(array.get(0)
                  .isObject()
                  .get(ID)
                  .isString()
                  .stringValue()));
              sb.append("/>");
            } else {
              sb.append("<div class='x-view-selected x-view-no-image'><label>");
              sb.append(CONSTANTS.noImageDisplay());
              sb.append("</label>");
            }
            sb.append("</div><br/><div><label>");
            sb.append(record.getAsString(SHORT_DESCRIPTION));
            sb.append("</label></div><br/><div><label style='float:right;'>");
            sb.append(CONSTANTS.createdBy() + "&nbsp;");
            sb.append(record.getAsString(CREATOR));
            sb.append("&nbsp;</label></div></div>");

            settingsPanel.setHtml(sb.toString());
          }

          public void onError(Request arg0, Throwable arg1) {
            settingsPanel.setHtml("Load error");
          }
        });

  }
}
