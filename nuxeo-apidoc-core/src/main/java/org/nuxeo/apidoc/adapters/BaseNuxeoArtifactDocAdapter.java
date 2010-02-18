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
package org.nuxeo.apidoc.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public abstract class BaseNuxeoArtifactDocAdapter extends BaseNuxeoArtifact{

     protected DocumentModel doc;

     protected static Log log = LogFactory.getLog(BaseNuxeoArtifactDocAdapter.class);

     protected static String computeDocumentName(String name) {
         return IdUtils.generateId(name,"-",true,500);
     }

     protected static String getRootPath(CoreSession session, String basePath, String suffix) throws ClientException {
         PathRef rootRef = new PathRef(basePath);
         if (session.exists(rootRef)) {
             Path path = new Path(basePath).append(suffix);
             rootRef = new PathRef(path.toString());
             if (session.exists(rootRef)) {
                 return path.toString();
             } else {
                 DocumentModel root = session.createDocumentModel("Folder");
                 root.setPathInfo(basePath, suffix);
                 root = session.createDocument(root);
                 return root.getPathAsString();
             }
         }
         return null;
     }

     public BaseNuxeoArtifactDocAdapter(DocumentModel doc) {
         this.doc=doc;
     }

     @Override
     public int hashCode() {
         return doc.getId().hashCode();
     }

     public DocumentModel getDoc() {
         return doc;
     }

     protected CoreSession getCoreSession() {
         if (doc == null) {
             return null;
         }
         return CoreInstance.getInstance().getSession(doc.getSessionId());
     }

     protected <T> T getParentNuxeoArtifact(Class <T> artifactClass ) {

         try {
             for (DocumentModel parent : getCoreSession().getParentDocuments(doc.getRef())) {

                 T result = parent.getAdapter(artifactClass);
                 if (result!=null) {
                     return result;
                 }
             }
         }
         catch (Exception e) {
             log.error("Error while getting Parent artifact", e);
             return null;
         }
         log.error("Parent artifact not found ");
         return null;
     }


}
