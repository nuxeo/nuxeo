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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.contribs.api.UniversProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocumentWrapper;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;

/**
 *
 * @author 10044893
 *
 */
public class DefaultUniversProvider implements UniversProvider{


  private static final Log log = LogFactory.getLog(DefaultUniversProvider.class);

  private static final String type=Constants.Univers.TYPE;


  public Univers create(Univers univers, CoreSession session) throws SpaceException {
    String universParentPath = Constants.Univers.ROOT_PATH;

    DocumentModel createdDocument = null;
    try {
      DocumentModel parent = session.getDocument(new PathRef(universParentPath));
      createdDocument = DocumentHelper.createInternalDocument(parent,
          univers.getName(), univers.getTitle(), univers.getDescription(),
          session,type);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }
    return createdDocument.getAdapter(Univers.class);
  }

  public void delete(Univers univers,CoreSession session) throws SpaceException {
    try {
      DocumentHelper.delete(((DocumentWrapper)univers).getInternalDocument(), session);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }

  }

  public List<Univers> getAllElements(CoreSession session) throws SpaceException {
    List<Univers> list = new ArrayList<Univers>();
    try {
      DocumentModelList docList = session.query("SELECT * FROM Document WHERE ecm:primaryType='Univers'");
      for (DocumentModel documentModel : docList) {
        list.add(documentModel.getAdapter(Univers.class));
      }
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }
    return list;
  }

  public Univers getElementByName(final String name, CoreSession session) throws NoElementFoundException,SpaceException {
    try {
      Filter nameFilter= new Filter(){

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public boolean accept(DocumentModel docModel) {
          return name!=null && docModel!=null && docModel.getName()!=null && docModel.getName().equals(name) ;
        }

      };
      DocumentModelList docList = session.query("SELECT * FROM Document WHERE ecm:primaryType='Univers'",nameFilter);
      if(docList==null || docList.size()==0){
        throw new NoElementFoundException("no univers found in the repository matching name '"+name+"'");
      }else {
        if(docList.size()>1){
          log.warn("More than one univers element matching name '"+name+"' were found in the repository :"+docList.size()+" elements !!");
        }
        return docList.get(0).getAdapter(Univers.class);
      }
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }

  }

  public Univers update(Univers univ, CoreSession session) throws SpaceException {
    DocumentModel documentModel;
    try {
      documentModel = DocumentHelper.getDocumentById(univ.getId(), session);
      documentModel = DocumentHelper.updateDocument(documentModel, session,univ.getName(),univ.getTitle(),univ.getDescription());

      session.saveDocument(documentModel);
      session.save();
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }
    return documentModel.getAdapter(Univers.class);
  }









}
