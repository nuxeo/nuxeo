package org.nuxeo.ecm.webengine.cmis;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.atompub.server.CMISCollectionForChildren;
import org.apache.chemistry.atompub.server.CMISProvider;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;


@WebObject(type="cmis")
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    static Repository repository;
    static Abdera abdera;
    static  CMISProvider provider;
    static CMISCollectionForChildren cc;

    public static void initialize() {
        repository = new NuxeoRepository("default");
        abdera = new Abdera();
        provider = new CMISProvider(repository);
        Map<String, String> properties = new HashMap<String, String>();
        provider.init(abdera, properties);
        cc = new CMISCollectionForChildren(CMIS.COL_ROOT_CHILDREN, repository.getInfo().getRootFolderId(), repository);
    }

  /**
   * Default view
   */
  @GET
  public Response doGet() {
      RequestContext reqCtx = new ServletRequestContext(provider, ctx.getRequest());

      ResponseContext respCtx = provider.process(reqCtx);
      return getResponse(respCtx);
  }

  public Response getResponse(ResponseContext context) {
      if (context == null) {
          return Response.status(500).build();
      }
      ResponseBuilder builder = Response.status(context.getStatus());
      long cl = context.getContentLength();
      String cc = context.getCacheControl();
      if (cl > -1) {
          builder.header("Content-Length", cl);
      }
      if (cc != null && cc.length() > 0) {
          builder.header("Cache-Control",cc);
      }
      MimeType ct = context.getContentType();
      if (ct != null) {
          builder.type(ct.toString());
      }
      String[] names = context.getHeaderNames();
      for (String name : names) {
          Object[] headers = context.getHeaders(name);
          for (Object value : headers) {
              builder.header(name, value);
//              if (value instanceof Date) {
//                  //TODO format header field here?
//                  builder.header(name, ((Date)value).getTime());
//              } else {
//                  builder.header(name, value.toString());
//              }
          }
      }
      return builder.build();
  }

}

