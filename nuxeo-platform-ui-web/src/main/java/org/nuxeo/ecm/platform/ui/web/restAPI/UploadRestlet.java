/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;

@Name("uploadRestlet")
@Scope(EVENT)
public class UploadRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = -7858792615823015193L;

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    @In(create = true)
    protected SimpleFileManager FileManageActions;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String fileName = (String) req.getAttributes().get("filename");

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        DocumentModel dm;

        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            dm = documentManager.getDocument(new IdRef(docid));
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        if (dm != null) {
            Representation repr = req.getEntity();
            RestletFileUpload fu = new RestletFileUpload();
            List<FileItem> fiList = null;
            try {
                fiList = fu.parseRequest(req);
            } catch (FileUploadException e) {
                //handleError(res, e);
                //return;
                // XXX : this fails for requests not sent via browser
            }

            if (fiList == null) {
                // mono import

                try {
                    InputStream input = repr.getStream();
                    MediaType mediaType = repr.getMediaType();
                    String mimeType = mediaType.getName();
                    byte[] content = FileUtils.readBytes(input);

                    try {
                        String returnCode = FileManageActions.addBinaryFile(
                                content, mimeType, fileName, new IdRef(docid));
                        Element upload = result.addElement("upload");
                        upload.setText(returnCode);
                        result.setRootElement(upload);

                    } catch (ClientException e) {
                        handleError(res, e);
                        return;
                    }
                    //
                } catch (IOException e) {
                    handleError(res, e);
                    return;
                }
            } else {
                // multiple file upload
                Element uploads = result.addElement("uploads");
                result.setRootElement(uploads);

                for (FileItem fileItem : fiList) {

                    Element upload = result.addElement("upload");
                    ((org.w3c.dom.Element) uploads).appendChild((org.w3c.dom.Element) upload);

                    fileName = fileItem.getName();
                    String mimeType = fileItem.getContentType();
                    byte[] content;
                    try {
                        content = FileUtils.readBytes(fileItem.getInputStream());
                    } catch (IOException e) {
                        upload.setText("ERROR : " + e.getMessage());
                        continue;
                    }

                    try {
                        String returnCode = FileManageActions.addBinaryFile(
                                content, mimeType, fileName, new IdRef(docid));
                        upload.setText(returnCode);
                    } catch (ClientException e) {
                        upload.setText("ERROR : " + e.getMessage());
                        continue;
                    }
                }
            }
        }

        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }

}
