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
import javax.ws.rs.core.MediaType;
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
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "GadgetDocument")
@Produces( { "application/octet-stream" })
public class GadgetDocument extends ModuleRoot {

  private static final Log log = LogFactory.getLog(GadgetDocument.class);
  private static final String GADGET_HTML_CONTENT = "gadget:htmlContent";

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
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);
        String xpath = "file:content";
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

          String contentDisposition = "attachment;filename=" + fileName;
          if(fileName.endsWith(".swf")) {
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
      return Response.status(404)
          .build();

    } catch (ClientException e) {
      return Response.serverError()
          .build();
    }

  }

  @POST
  @Path("/ajaxSubmit/{gadgetid}")
  public Response ajaxSubmit(@PathParam("gadgetid") String id) {
    CoreSession session = ctx.getCoreSession();
    IdRef ref = new IdRef(id);
    try {
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);

        FormData form = ctx.getForm();
        form.fillDocument(doc);

        if (form.isMultipartContent()) {
          String xpath = "file:content";
          Blob blob = form.getFirstBlob();
          if (blob == null) {
            throw new IllegalArgumentException(
                "Could not find any uploaded file");
          } else if (blob.getLength() == -1) {
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
        session.saveDocument(doc);
        session.save();

        return Response.ok()
            .build();
      }

    } catch (ClientException e) {
      log.error(e);
    }
    return Response.serverError()
        .build();
  }

  @GET
  @Path("/getHtmlContent/{gadgetid}")
  public Response getHtmlContent(@PathParam("gadgetid") String id) {
    CoreSession session = ctx.getCoreSession();
    try {
      IdRef ref = new IdRef(id);
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);
        String htmlContent = (String) doc.getPropertyValue(GADGET_HTML_CONTENT);
        return Response.ok(htmlContent, MediaType.TEXT_HTML)
            .build();
      }
      return Response.serverError()
          .build();

    } catch (ClientException e) {
      return Response.serverError()
          .build();
    }

  }

  @POST
  @Path("/setHtmlContent/{gadgetid}")
  public Response setHtmlContent(@PathParam("gadgetid") String id) {
    CoreSession session = ctx.getCoreSession();
    IdRef ref = new IdRef(id);
    try {
      if (session.exists(ref)) {
        DocumentModel doc = session.getDocument(ref);
        FormData form = ctx.getForm();
        doc.setPropertyValue(GADGET_HTML_CONTENT,
            form.getString(GADGET_HTML_CONTENT));
        session.saveDocument(doc);
        session.save();

        return Response.ok()
            .build();
      }
    } catch (ClientException e) {
      log.error(e);
    }
    return Response.serverError()
        .build();
  }
}
