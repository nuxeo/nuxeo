package org.nuxeo.opensocial.gadgets;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "GadgetDocument")
@Produces( { "application/octet-stream" })
public class GadgetDocument extends ModuleRoot {

  private static final Log log = LogFactory.getLog(GadgetDocument.class);

  @GET
  public Object noRootRessource() {
    return Response.serverError();
  }

  @GET
  @Path("/getFile/{gadgetid}")
  public Response getFile(@PathParam("gadgetid") String id) {
    CoreSession session = ctx.getCoreSession();
    try {
      StringTokenizer st = new StringTokenizer(id, ".");
      IdRef ref = new IdRef(st.nextToken());
      log.info("produces image/jpeg,image/gif");
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);
        FormData form = ctx.getForm();
        String xpath = form.getString(FormData.PROPERTY);
        if (xpath == null) {
          if (doc.hasSchema("file")) {
            xpath = "file:content";
          } else {
            throw new IllegalParameterException(
                "Missing request parameter named 'property' that specify the blob property xpath to fetch");
          }
        }
        try {
          Property p = doc.getProperty(xpath);
          Blob blob = (Blob) p.getValue();

          if (blob == null) {
            throw new WebResourceNotFoundException("No attached file at "
                + xpath);
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
          }
          log.info("mime type " + blob.getMimeType());
          return Response.ok(blob)
              .header("Content-Disposition", "attachment;filename=" + fileName)
              .type(blob.getMimeType())
              .build();
        } catch (Exception e) {
          throw WebException.wrap("Failed to get the attached file", e);
        }

      }
      return Response.status(404)
          .build();

    } catch (ClientException e) {
      return Response.serverError()
          .build();
    }

  }

  @POST
  @Path("/setFile/{gadgetid}")
  public Response setFile(@PathParam("gadgetid") String id) {
    CoreSession session = ctx.getCoreSession();
    IdRef ref = new IdRef(id);
    try {
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);

        FormData form = ctx.getForm();

        form.fillDocument(doc);
        String xpath = ctx.getForm()
            .getString(FormData.PROPERTY);
        if (xpath == null) {
          if (doc.hasSchema("file")) {
            xpath = "file:content";
          } else {
            throw new IllegalArgumentException(
                "Missing request parameter named 'property' that specifies "
                    + "the blob property xpath to fetch");
          }
        }
        Blob blob = form.getFirstBlob();
        if (blob == null) {
          throw new IllegalArgumentException("Could not find any uploaded file");
        }
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
          session.saveDocument(doc);
          session.save();

          return Response.ok()
              .build();
        } catch (WebException e) {
          throw e;
        } catch (Exception e) {
          throw WebException.wrap("Failed to attach file", e);
        }
      }
    } catch (ClientException e) {
      log.error(e);
    }
    return Response.serverError()
        .build();
  }
}
