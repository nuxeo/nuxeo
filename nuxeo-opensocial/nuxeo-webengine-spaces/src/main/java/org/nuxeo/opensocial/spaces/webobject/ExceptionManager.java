package org.nuxeo.opensocial.spaces.webobject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceElementNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;

/**
 * Exception manager  allowing to treat
 * SpaceSecurityException as WebSecurityException and
 * SpaceElementNotFoundException as WebResourceNotFoundException
 */
public class ExceptionManager {

  private static final Log LOGGER = LogFactory.getLog(ExceptionManager.class);
  /**
   * Replacing/completing function for 'WebException.wrap' allowing to treat
   * SpaceSecurityException as WebSecurityException and
   * SpaceElementNotFoundException as WebResourceNotFoundException
   */
  public static WebException wrap(Throwable e) {

    //traitement specifique dans le cas d une SpaceSecurityException
    if(e instanceof SpaceSecurityException ){
      LOGGER.error("SpaceSecurityException "+e.getMessage(),e);
      return new WebSecurityException(e.getMessage(), e);
    }

    //traitement des 'not founds'
    if(e instanceof SpaceElementNotFoundException){
      LOGGER.error("SpaceElementNotFoundException "+e.getMessage(),e);
      return new WebResourceNotFoundException(e.getMessage(), e);
    }

    //traitement par defaut
    LOGGER.error("Unhandled error :"+e.getMessage(),e);
    throw WebException.wrap(e);
  }

}
