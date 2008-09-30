/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.model;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.annotations.Application;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MainResource {

    protected WebContext2 ctx;
    protected WebApplication app;
    
  
    public MainResource() {
        ctx = WebEngine2.getActiveContext();
        app = ctx.getEngine().getApplication(getApplicationName());
        ctx.setApplication(app);        
        //TODO: invoke application guard if any

        System.out.println("@@@ MAIN2: "+getClass()+" >> ancestors res>> "+ctx.getUriInfo().getMatchedResources());
      System.out.println("@@@ MAIN2: "+getClass()+" >> ancestors uris>> "+ctx.getUriInfo().getMatchedURIs());

    }
    
    public WebApplication getApplication() {
        return app;
    }
    
    public WebContext2 getContext() {
        return ctx; 
    }    
    
    protected String getApplicationName() {
        Application anno = (Application)this.getClass().getAnnotation(Application.class);
        if (anno != null) {
            return anno.name();
        }
        throw new UnsupportedOperationException("This method must be implement by derived main resource classes");
    }

    @Path(value="{segment}")
    public Object dispatch(@PathParam("segment") String segment) throws WebException {
      System.out.println("appdispatch>>>>>>>>>>>> "+segment);
      Object result = null; 
      if (segment.startsWith("@")) {
          System.out.println("appdispatch>>>>>>>>>>>> "+segment+" - dispatch action");
          result = resolveAction(segment.substring(1));    
      } else {
          System.out.println("appdispatch>>>>>>>>>>>> "+segment+" - dispatch object");
          result = resolveObject(segment);
      }
      if (result == null) {
          throw new NoSuchResourceException(segment);
      }
      return  result;
    }
    
    protected WebAction resolveAction(String actionName) throws WebException {
        throw new NoSuchResourceException("No Such Action: "+actionName);
    }
    
    protected WebObject resolveObject(String segment) throws WebException {
        throw new NoSuchResourceException("No Such Object: "+segment);
    }
    
    public WebObject newObject(String type, String path) throws WebException {
        return ctx.newObject(type, path);
    }
    
    public String getPath() {
        return "/"; //TODO 
    }
    
}
