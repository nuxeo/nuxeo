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

package org.nuxeo.ecm.spaces.core.contribs.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.contribs.api.GadgetProvider;
import org.nuxeo.ecm.spaces.core.impl.AbstractProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocumentWrapper;

/**
 * 
 * @author 10044893
 * 
 */
public class DefaultGadgetProvider extends AbstractProvider<Gadget, Space>
    implements GadgetProvider {

  private static final Log log = LogFactory.getLog(DefaultGadgetProvider.class);

  public DefaultGadgetProvider() {
    super(Gadget.class, Constants.Gadget.TYPE, "defaultGadget");
  }

  @Override
  public void delete(Gadget element, CoreSession session) throws SpaceException {
    try {
      DocumentModel documentModel = DocumentHelper.getDocumentById(
          element.getId(), session);
      DocumentHelper.delete(documentModel, session);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    } catch (ClientException e) {
      throw new SpaceException(e);
    }
  }

  public Gadget create(Gadget data, Space parent, CoreSession session)
      throws SpaceException {
    try {

      // common creation

      DocumentModel doc = DocumentHelper.createInternalDocument(
          ((DocumentWrapper) parent).getInternalDocument(), data.getName(),
          data.getTitle(), data.getDescription(), session,
          Constants.Gadget.TYPE);

      // specific creation
      doc = complete(doc, data);

      // save document + session
      session.saveDocument(doc);
      session.save();
      return getAdaptedDocument(doc);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    } catch (ClientException e) {
      throw new SpaceException(e);
    }
  }

  public Gadget update(Gadget newDatas, CoreSession session)
      throws SpaceException {
    try {
      DocumentModel documentModel = DocumentHelper.getDocumentById(
          newDatas.getId(), session);

      // common update
      documentModel = DocumentHelper.updateDocument(documentModel, session,
          newDatas.getName(), newDatas.getTitle(), newDatas.getDescription());

      // specific update
      documentModel = complete(documentModel, newDatas);
      // save document + session
      session.saveDocument(documentModel);
      session.save();
      return documentModel.getAdapter(Gadget.class);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    } catch (ClientException e) {
      throw new SpaceException(e);
    }

  }

  private DocumentModel complete(DocumentModel docToCreate, Gadget x)
      throws PropertyException, ClientException {

    docToCreate.setPropertyValue(Constants.Gadget.GADGET_CATEGORY,
        x.getCategory());

    docToCreate.setPropertyValue(Constants.Gadget.GADGET_PLACEID,
        x.getPlaceID());

    docToCreate.setPropertyValue(Constants.Gadget.GADGET_POSITION,
        x.getPosition());

    docToCreate.setPropertyValue(Constants.Gadget.GADGET_HEIGHT, x.getHeight());

    docToCreate.setPropertyValue(Constants.Gadget.GADGET_HTMLCONTENT,
        x.getHtmlContent());

    Property p = docToCreate.getProperty(Constants.Gadget.GADGET_PREFERENCES);
    if (p.isList()) {
      if (p.getSchema()
          .getName()
          .equals("gadget")) {
        Map<String, String> map = new HashMap<String, String>();
        p.setValue(null);
        Map<String, String> preferences = x.getPreferences();
        if (preferences != null) {
          Set<String> set = preferences.keySet();
          for (String key : set) {
            String value = preferences.get(key);
            if (value != null) {
              try {
                map.put("value", URLDecoder.decode(value, "UTF-8"));
                map.put("name", key);
                p.add(map);
              } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);
              }
            }
          }
        }
      }
    }
    docToCreate.setPropertyValue(Constants.Gadget.GADGET_COLLAPSED,
        x.isCollapsed());
    return docToCreate;
  }

}
