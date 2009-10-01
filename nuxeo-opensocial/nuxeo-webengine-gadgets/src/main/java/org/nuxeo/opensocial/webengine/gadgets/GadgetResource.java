package org.nuxeo.opensocial.webengine.gadgets;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public class GadgetResource extends InputStreamResource {

  static {
    try {
      WebEngine we = Framework.getService(WebEngine.class);
      we.getRegistry()
          .addMessageBodyWriter(new GadgetStreamWriter());
    } catch (Exception e) {

    }
  }

  GadgetDeclaration gadget;

  public GadgetResource(GadgetDeclaration gadget) {
    this.gadget = gadget;
  }

  @GET
  @Path("{filename:.*}")
  public Object getGadgetFile(@PathParam("filename") String fileName)
      throws Exception {
    InputStream in = Framework.getService(GadgetService.class)
        .getGadgetResource(gadget.getName(), fileName);
    return getObject(in, fileName);
  }
}
