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

package org.nuxeo.ecm.spaces.core.impl;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

public final class DocumentHelper {


  private DocumentHelper(){}

  public static DocumentModel createInternalDocument(DocumentModel parent,
      String id, String title, String description, CoreSession session,
      String type) throws ClientException {

    DocumentModel docToCreate = session.createDocumentModel(
        parent.getPathAsString(), id, type);
    docToCreate.setPropertyValue(Constants.Document.DOCUMENT_TITLE, id);
    docToCreate = session.createDocument(docToCreate);
    docToCreate.setPropertyValue(Constants.Document.DOCUMENT_TITLE, title);
    docToCreate.setPropertyValue(Constants.Document.DOCUMENT_DESCRIPTION,
        description);
    session.saveDocument(docToCreate);
    session.save();
    return docToCreate;
  }

  public static void delete(DocumentModel dm, CoreSession session)
      throws ClientException {
    session.removeDocument(dm.getRef());
    session.save();
  }


  public static DocumentModel updateDocument(DocumentModel documentModel,
      CoreSession session, String... args) throws ClientException {

    String name = args[0];
    String title = args[1];
    String desc = args[2];
    DocumentModel movedDoc  = documentModel;
    if (!name.equals(documentModel.getName())) {
      movedDoc = session.move(documentModel.getRef(),
          documentModel.getParentRef(), IdUtils.generateId(name));
    }
    movedDoc.setPropertyValue("dc:description", desc);
    movedDoc.setPropertyValue("dc:title", title);

    return movedDoc;
  }

  public static  DocumentModel getDocumentById(String id, CoreSession session) throws ClientException{
    return session.getDocument(new IdRef(id));
  }


}
