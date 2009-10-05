/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

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
