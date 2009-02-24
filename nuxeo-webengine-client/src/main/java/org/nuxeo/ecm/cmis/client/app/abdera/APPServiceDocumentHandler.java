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
 */
package org.nuxeo.ecm.cmis.client.app.abdera;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.cmis.CmisRepositoryInfo;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.nuxeo.ecm.cmis.client.app.APPContentManager;
import org.nuxeo.ecm.cmis.client.app.APPServiceDocument;
import org.nuxeo.ecm.cmis.client.app.AppRepository;
import org.nuxeo.ecm.cmis.client.app.SerializationHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPServiceDocumentHandler implements SerializationHandler<APPServiceDocument> {

    protected Abdera abdera;
    
    protected APPContentManager contentManager;
    
    public APPServiceDocumentHandler(APPContentManager contentManager, Abdera abdera) {
        this.contentManager = contentManager;
        this.abdera = abdera;
    }
    
    public Class<APPServiceDocument> getObjectType() {
        return APPServiceDocument.class;
    }

    public String getContentType() {
        return "application/atom+xml";
    }
    
    public APPServiceDocument read(InputStream in) throws IOException {
        Document<Service> document = abdera.getParser().parse(in);
        Service atomService = document.getRoot();
        List<Workspace>  atomWorkspaces = atomService.getWorkspaces();
        AppRepository repos[] = new AppRepository[atomWorkspaces.size()];
        int i = 0;
        for (i = 0; i < repos.length; i++) {
            repos[i] = new AppRepository(contentManager, 
                    atomWorkspaces.get(i).getExtension(CmisRepositoryInfo.class).getRepositoryId());
        }
        return new APPServiceDocument(repos);
    }
    
    public void write(APPServiceDocument object, OutputStream out)
            throws IOException {
        throw new UnsupportedOperationException("Write content is not available for APPServiceDocument");
    }

}
