package org.nuxeo.ecm.webengine.cmis;

import java.io.*;
import java.net.ResponseCache;
import java.util.Date;

import javax.activation.MimeType;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.atompub.server.CMISCollection;
import org.apache.chemistry.atompub.server.CMISCollectionForChildren;
import org.apache.chemistry.atompub.server.CMISProvider;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;

import com.sun.corba.se.impl.protocol.RequestCanceledException;

@WebObject(type="cmis")
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    static Repository repository;
    static Abdera abdera;
    static  CMISProvider provider;
    static CMISCollectionForChildren cc;
    
    public static void initialize() {
        repository = null;
        abdera = new Abdera();
        provider = new CMISProvider(repository);
        cc = new CMISCollectionForChildren(CMIS.COL_ROOT_CHILDREN, repository.getInfo().getRootFolderId(), repository);        
    }
    
  /**
   * Default view
   */
  @GET
  public Object doGet() {
      RequestContext reqCtx = new ServletRequestContext(provider, ctx.getRequest());
      return provider.process(reqCtx);
  }

  
  protected void output(
          HttpServletRequest request, 
          HttpServletResponse response, 
          ResponseContext context)
            throws IOException {
          if (context != null) {
            response.setStatus(context.getStatus());
            long cl = context.getContentLength();
            String cc = context.getCacheControl();
            if (cl > -1) response.setHeader("Content-Length", Long.toString(cl));
            if (cc != null && cc.length() > 0) response.setHeader("Cache-Control",cc);
            try {
              MimeType ct = context.getContentType();
              if (ct != null) response.setContentType(ct.toString());
            } catch (Exception e) {}
            String[] names = context.getHeaderNames();
            for (String name : names) {
              Object[] headers = context.getHeaders(name);
              for (Object value : headers) {          
                if (value instanceof Date)
                  response.setDateHeader(name, ((Date)value).getTime());
                else
                  response.setHeader(name, value.toString());
              }
            }
            
            if (!request.getMethod().equals("HEAD") && context.hasEntity()) {
              context.writeTo(response.getOutputStream());
            }  
          } else {
              throw new WebException("Internal Server Error");
          }
        }
  
}

