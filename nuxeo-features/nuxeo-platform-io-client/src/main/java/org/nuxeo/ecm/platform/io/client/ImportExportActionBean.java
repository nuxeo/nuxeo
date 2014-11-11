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
 * $Id$
 */

package org.nuxeo.ecm.platform.io.client;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.platform.io.selectionReader.DocumentModelListReader;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;

@Name("importExportAction")
@Scope(ScopeType.EVENT)
public class ImportExportActionBean {

    private static final String RESTLET_PREFIX = "restAPI";

    private static final Log log = LogFactory.getLog(ImportExportActionBean.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected ClipboardActions clipboardActions;

    private static StringBuffer getRestletBaseURL(DocumentModel doc) {
        StringBuffer urlb = new StringBuffer();

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
        ServletResponse response = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            response = (ServletResponse) facesContext.getExternalContext()
                    .getResponse();
        }

        if (response != null && response instanceof HttpServletResponse) {
            return (HttpServletResponse) response;
        }
        return null;
    }

    private static HttpServletRequest getHttpServletRequest() {
        ServletRequest request = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            request = (ServletRequest) facesContext.getExternalContext()
                    .getRequest();
        }

        if (request != null && request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }

    private static void handleRedirect(HttpServletResponse response, String url)
            throws IOException {

        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(
                URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY, true);
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
     * Generates URL to call export restlet on a leaf.
     *
     * @return export restlet URL
     */
    public String getDocumentExportURL() {
        StringBuffer urlb = getRestletBaseURL(
                navigationContext.getCurrentDocument());
        urlb.append("exportTree?format=XML");
        return urlb.toString();
    }

    /**
     * Generates URL to call export restlet on a folder.
     *
     * @return export restlet URL
     */
    public String getFolderExportURL() {
        StringBuffer urlb = getRestletBaseURL(
                navigationContext.getCurrentDocument());
        urlb.append("exportTree?format=ZIP");
        return urlb.toString();
    }

    public String exportCurrentList() {
        List<DocumentModel> docList = clipboardActions.getCurrentSelectedList();
        if (docList != null) {
            export(docList);
        }
        return null;
    }

    public static void export(List<DocumentModel> docList) {
        HttpServletResponse response = getHttpServletResponse();
        if (response == null) {
            return;
        }
        DocumentReader reader = null;
        DocumentWriter writer = null;
        try {

            reader = new DocumentModelListReader(docList);

            response.reset();
            writer = new NuxeoArchiveWriter(response.getOutputStream());

            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);

            pipe.run();

            String filename="export.zip";
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + filename + "\";");
            response.setHeader("Content-Type","application/zip");
            FacesContext.getCurrentInstance().responseComplete();

        } catch (ClientException e) {
            log.error("Error durinh XML export " + e.getMessage());
        } catch (IOException e) {
            log.error("Error durinh XML export " + e.getMessage());
        } catch (Exception e) {
            log.error("Error durinh XML export " + e.getMessage());
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

}
