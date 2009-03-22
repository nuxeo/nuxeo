/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.cmis;

import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.apache.abdera.util.Constants;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.atompub.server.CMISCollectionForChildren;
import org.apache.chemistry.atompub.server.CMISProvider;
import org.apache.chemistry.atompub.server.CMISServiceResponse;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;


@WebObject(type="cmis")
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    static Repository repository;
    static Abdera abdera;
    static  CMISProvider provider;
    static CMISCollectionForChildren cc;

    static {
        initialize();
    }

    public static void initialize() {
        repository = new NuxeoRepository("default");
        abdera = new Abdera();
        provider = new CMISProvider(repository);
        Map<String, String> properties = new HashMap<String, String>();
        provider.init(abdera, properties);
        cc = new CMISCollectionForChildren(CMIS.COL_ROOT_CHILDREN, repository.getInfo().getRootFolderId(), repository);

        Framework.getLocalService(WebEngine.class).getRegistry().addMessageBodyWriter(new AbderaResponseWriter());
    }

  /**
   * Default view
   */
  @GET
  public Response doGet() {
      RequestContext reqCtx = new ServletRequestContext(provider, ctx.getRequest());
      CMISServiceResponse response = new CMISServiceResponse(provider, reqCtx);
      response.setStatus(200);
      response.setContentType(Constants.APP_MEDIA_TYPE);
      return getResponse(response);
  }

  @GET
  @Path("children")
  public Response doGetFeed() {
      RequestContext reqCtx = new ServletRequestContext(provider, ctx.getRequest());
      return getResponse(cc.getFeed(reqCtx));
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
      return builder.entity(context).build();
  }

}
