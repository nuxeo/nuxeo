package org.nuxeo.opensocial.webengine.gadgets;

import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

public class InputStreamResource {
  static {
    try {
      WebEngine we = Framework.getService(WebEngine.class);
      we.getRegistry()
          .addMessageBodyWriter(new GadgetStreamWriter());
    } catch (Exception e) {

    }
  }

  protected Response getObject(InputStream gadgetResource, String path) {
    if (gadgetResource == null) {
      return Response.ok(404)
          .build();
    }

    int p = path.lastIndexOf('.');
    if (p > -1) {
      String mime = WebEngine.getActiveContext()
          .getEngine()
          .getMimeType(path.substring(p + 1));
      if (mime == null) {
        if (path.endsWith(".xsd")) {
          mime = "text/xml";
        }
      }

      // To Avoid a small bug....
      if ("text/plain".equals(mime)) {
        mime = "text/html";
      }

      return Response.ok(new GadgetStream(gadgetResource))
          .type(mime)
          .build();
    }
    return Response.ok(new GadgetStream(gadgetResource))
        .type("application/octet-stream")
        .build();

  }
}
