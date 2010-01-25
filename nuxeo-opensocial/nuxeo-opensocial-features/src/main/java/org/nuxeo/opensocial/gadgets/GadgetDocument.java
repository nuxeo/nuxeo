package org.nuxeo.opensocial.gadgets;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

@WebObject(type = "GadgetDocument")
@Produces( { "application/octet-stream" })
public class GadgetDocument extends DocumentObject {

  private static final String GADGET_HTML_CONTENT = "gadget:htmlContent";

  @GET
  @Override
  public Object doGet() {
    return Response.serverError();
  }

  @POST
  @Override
  public Response doPost() {
    FormData form = ctx.getForm();
    form.fillDocument(doc);

    if (form.isMultipartContent()) {
      String xpath = "file:content";
      Blob blob = form.getFirstBlob();
      if (blob == null) {
        throw new IllegalArgumentException("Could not find any uploaded file");
      } else {
        try {
          Property p = doc.getProperty(xpath);
          if (p.isList()) {
            if ("files".equals(p.getSchema()
                .getName())) {
              Map<String, Serializable> map = new HashMap<String, Serializable>();
              map.put("filename", blob.getFilename());
              map.put("file", (Serializable) blob);
              p.add(map);
            } else {
              p.add(blob);
            }
          } else {
            if ("file".equals(p.getSchema()
                .getName())) {
              p.getParent()
                  .get("filename")
                  .setValue(blob.getFilename());
            }
            p.setValue(blob);
          }

        } catch (WebException e) {
          throw e;
        } catch (Exception e) {
          throw WebException.wrap("Failed to attach file", e);
        }
      }
    }
    try {
      CoreSession session = getContext().getCoreSession();
      session.saveDocument(doc);
      session.save();
    } catch (ClientException e) {
      throw WebException.wrap(e);
    }

    return Response.ok()
        .build();

  }

  @GET
  @Path("file")
  public Object getFile() {
    try {
      Blob blob = getBlobFromDoc(doc);
      String filename = blob.getFilename();

      String contentDisposition = "attachment;filename=" + filename;

      // Special handling for SWF file. Since Flash Player 10, Flash player
      // ignores reading if it sees Content-Disposition: attachment
      // http://forum.dokuwiki.org/thread/2894
      if (filename.endsWith(".swf")) {
        contentDisposition = "inline;";
      }

      return Response.ok(blob)
          .header("Content-Disposition", contentDisposition)
          .type(blob.getMimeType())
          .build();
    } catch (Exception e) {
      throw WebException.wrap("Failed to get the attached file", e);
    }
  }

  private Blob getBlobFromDoc(DocumentModel doc) throws ClientException {
    String xpath = "file:content";

    Property p = doc.getProperty(xpath);
    Blob blob = (Blob) p.getValue();

    if (blob == null) {
      throw new WebResourceNotFoundException("No attached file at " + xpath);
    }
    String fileName = blob.getFilename();
    if (fileName == null) {
      p = p.getParent();
      if (p.isComplex()) { // special handling for file and files
        // schema
        try {
          fileName = (String) p.getValue("filename");
        } catch (PropertyException e) {
          fileName = "Unknown";
        }
      }
      blob.setFilename(fileName);
    }

    return blob;
  }

  @GET
  @Path("html")
  public Object doGetHtml() throws PropertyException, ClientException {
    String htmlContent = (String) doc.getPropertyValue(GADGET_HTML_CONTENT);
    return Response.ok(htmlContent, MediaType.TEXT_HTML)
        .build();
  }

}
