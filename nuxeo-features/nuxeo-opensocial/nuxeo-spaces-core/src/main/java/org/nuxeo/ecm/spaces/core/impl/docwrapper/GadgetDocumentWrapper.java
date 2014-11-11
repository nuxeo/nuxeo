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

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class GadgetDocumentWrapper extends DocumentWrapper implements
      Gadget {

  GadgetDocumentWrapper(DocumentModel doc) {
    super(doc);
  }

  private static final Log log = LogFactory
      .getLog(GadgetDocumentWrapper.class);



  private static final long serialVersionUID = 1L;

  public String getCategory() {
    return getInternalStringProperty(Constants.Gadget.GADGET_CATEGORY);
  }

  public String getPlaceID() {
    return getInternalStringProperty(Constants.Gadget.GADGET_PLACEID);
  }

  public int getPosition() {
    return getInternalIntegerProperty(Constants.Gadget.GADGET_POSITION);
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getPreferences() {


    try {
      ArrayList<Map<String, String>> list = (ArrayList<Map<String, String>>) getInternalDocument().getPropertyValue(Constants.Gadget.GADGET_PREFERENCES);
      if(list==null)
        return null;
      HashMap ret = new HashMap<String, String>();
      for (Map<String, String> map : list) {
        String key = map.get("name");
        String value= map.get("value");
        ret.put(key, value);
      }
      return ret ;
    } catch (PropertyException e) {
     log.error(e);
    } catch (ClientException e) {
      log.error(e);
    }
    return null;
  }

  //public String getType() {
   // return getInternalStringProperty(Constants.Gadget.GADGET_TYPE);
 // }

  public boolean isCollapsed() {
    return getInternalBooleanProperty(Constants.Gadget.GADGET_COLLAPSED);
  }

  public boolean isEqualTo(Gadget gadget) {
    return gadget.getId()!=null && gadget.getId().equals(getId());
  }


}
