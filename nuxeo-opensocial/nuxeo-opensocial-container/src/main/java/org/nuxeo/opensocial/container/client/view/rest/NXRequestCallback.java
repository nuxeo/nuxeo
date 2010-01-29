package org.nuxeo.opensocial.container.client.view.rest;

import org.nuxeo.opensocial.container.client.view.GadgetForm;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.gwtext.client.widgets.form.TextField;

public class NXRequestCallback implements RequestCallback {

  private TextField field;

  public NXRequestCallback(TextField field) {
    this.field = field;
  }

  public void onError(Request arg0, Throwable arg1) {
    // TODO Auto-generated method stub

  }

  public void onResponseReceived(Request arg0, Response rep) {
    new NXDataWindow(JSONParser.parse(rep.getText())
        .isObject()
        .get("data")
        .isArray(), GadgetForm.window, this.field);

  }

}
