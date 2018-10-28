/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.io.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.platform.io.selectionReader.DocumentModelListReader;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;

@Name("importExportAction")
@Scope(ScopeType.EVENT)
public class ImportExportActionBean implements Serializable {

    private static final String RESTLET_PREFIX = "restAPI";

    private static final Log log = LogFactory.getLog(ImportExportActionBean.class);

    private static final long serialVersionUID = 1770386525984671333L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient ClipboardActions clipboardActions;

    private static StringBuilder getRestletBaseURL(DocumentModel doc) {
        StringBuilder urlb = new StringBuilder();

        urlb.append(BaseURL.getBaseURL());
        urlb.append(RESTLET_PREFIX);
        urlb.append('/');
        urlb.append(doc.getRepositoryName());
        urlb.append('/');
        urlb.append(doc.getRef().toString());
        urlb.append('/');
        return urlb;
    }

    private static HttpServletResponse getHttpServletResponse() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext == null ? null : (HttpServletResponse) facesContext.getExternalContext().getResponse();
    }

    private static HttpServletRequest getHttpServletRequest() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        return (HttpServletRequest) facesContext.getExternalContext().getRequest();
    }

    private static void handleRedirect(HttpServletResponse response, String url) throws IOException {
        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
        FacesContext.getCurrentInstance().responseComplete();
    }

    public String doExportDocument() throws IOException {
        HttpServletResponse response = getHttpServletResponse();
        if (response != null) {
            handleRedirect(response, getDocumentExportURL());
        }
        return null;
    }

    public String doExportFolder() throws IOException {
        HttpServletResponse response = getHttpServletResponse();
        if (response != null) {
            handleRedirect(response, getFolderExportURL());
        }
        return null;
    }

    /**
     * Returns the REST URL for export of given document.
     *
     * @since 5.4.2
     * @param doc the document to export
     * @param exportAsZip a boolean stating if export should be given in ZIP format. When exporting the tree, ZIP format
     *            is forced.
     * @param exportAsTree a boolean stating if export should include the document children.
     */
    public String getExportURL(DocumentModel doc, boolean exportAsZip, boolean exportAsTree) {
        if (doc == null) {
            return null;
        }
        StringBuilder urlb = getRestletBaseURL(doc);
        if (exportAsTree) {
            urlb.append("exportTree");
        } else {
            if (exportAsZip) {
                urlb.append("export?format=ZIP");
            } else {
                urlb.append("export?format=XML");
            }
        }
        return urlb.toString();
    }

    /**
     * Generates URL to call export restlet on a leaf.
     *
     * @return export restlet URL
     */
    public String getDocumentExportURL() {
        return getExportURL(navigationContext.getCurrentDocument(), true, true);
    }

    /**
     * Generates URL to call export restlet on a folder.
     *
     * @return export restlet URL
     */
    public String getFolderExportURL() {
        return getExportURL(navigationContext.getCurrentDocument(), true, true);
    }

    /**
     * Returns the Rest URL for a document export in XML format
     *
     * @since 5.4.2
     */
    public String getDocumentXMLExportURL() {
        return getExportURL(navigationContext.getCurrentDocument(), false, false);
    }

    /**
     * Returns the Rest URL for a document export in ZIP format
     *
     * @since 5.4.2
     */
    public String getDocumentZIPExportURL() {
        return getExportURL(navigationContext.getCurrentDocument(), true, false);
    }

    /**
     * Returns the Rest URL for a document tree export in ZIP format
     *
     * @since 5.4.2
     * @return
     */
    public String getDocumentZIPTreeExportURL() {
        return getExportURL(navigationContext.getCurrentDocument(), true, true);
    }

    public String exportCurrentList() {
        List<DocumentModel> docList = clipboardActions.getCurrentSelectedList();
        if (docList != null) {
            export(docList);
        }
        return null;
    }

    public static void export(List<DocumentModel> docList) {
        DocumentReader reader = null;
        DocumentWriter writer = null;
        Blob blob = null;
        try {
            reader = new DocumentModelListReader(docList);
            blob = Blobs.createBlobWithExtension("zip");
            writer = new NuxeoArchiveWriter(blob.getFile());
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } catch (IOException e) {
            log.error("Error during XML export " + e.getMessage());
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        if (blob != null) {
            ComponentUtils.download(null, null, blob, "export.zip", "workListXML");
            if (blob.getFile() != null) {
                blob.getFile().delete();
            }
        }

    }

}
