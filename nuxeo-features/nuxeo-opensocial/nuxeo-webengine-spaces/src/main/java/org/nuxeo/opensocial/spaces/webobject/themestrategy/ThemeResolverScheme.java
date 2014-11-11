package org.nuxeo.opensocial.spaces.webobject.themestrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.negotiation.Scheme;

public class ThemeResolverScheme implements Scheme {



  private static final Log log = LogFactory.getLog(ThemeResolverScheme.class);

  public String getOutcome(Object arg0) {

    log.info(arg0);
    if(arg0 instanceof WebContext){
      WebContext ctx = (WebContext)arg0;
      String themeView = (String) ctx.getProperty("view.theme");
      log.info(themeView);
      if(themeView!=null && !themeView.trim().equals("")){
        return themeView+"/default";
      }
    }
    return null;
  }

}
