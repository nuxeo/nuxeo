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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Restlet to help LiveEdit clients update the blob content of a document
 *
 * @author Sun Tan <stan@nuxeo.com>
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@Name("uploadFileRestlet")
@Scope(EVENT)
public class UploadFileRestlet extends BaseNuxeoRestlet implements LiveEditConstants, Serializable {

    public static final String LIVED_AUTOVERSIONING_PROP = "org.nuxeo.ecm.platform.liveedit.autoversioning";

    public static final String POLICY_MINOR_INCR = "minor";

    private static final long serialVersionUID = -6167207806181917456L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @SuppressWarnings("deprecation")
    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String filename = (String) req.getAttributes().get("filename");
        try {
            filename = URLDecoder.decode(filename, URL_ENCODE_CHARSET);
        } catch (UnsupportedEncodingException e) {
            handleError(res, e);
            return;
        }

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel dm = null;
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            if (docid != null) {
                dm = documentManager.getDocument(new IdRef(docid));
            }
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        try {

            String blobPropertyName = getQueryParamValue(req, BLOB_PROPERTY_NAME, null);

            if (blobPropertyName == null) {
                // find the names of the fields from the optional request
                // parameters with fallback to defaults if none is provided
                String schemaName = getQueryParamValue(req, SCHEMA, DEFAULT_SCHEMA);
                String blobFieldName = getQueryParamValue(req, BLOB_FIELD, DEFAULT_BLOB_FIELD);
                blobPropertyName = schemaName + ":" + blobFieldName;
            }

            InputStream is = req.getEntity().getStream();

            saveFileToDocument(filename, dm, blobPropertyName, is);
        } catch (NuxeoException | IOException e) {
            handleError(res, e);
        }
    }

    protected CoreSession getDocumentManager() {
        return documentManager;
    }

    /**
     * Save the file into the document.
     *
     * @deprecated since 9.1 filename is now stored in blob
     */
    @Deprecated
    protected void saveFileToDocument(String filename, DocumentModel dm, String blobPropertyName,
            String filenamePropertyName, InputStream is) throws IOException, PropertyException {
        saveFileToDocument(filename, dm, blobPropertyName, is);
    }

    /**
     * Save the file into the document.
     */
    protected void saveFileToDocument(String filename, DocumentModel dm, String blobPropertyName,
            InputStream is) throws IOException, PropertyException {
        // persisting the blob makes it possible to read the binary content
        // of the request stream several times (mimetype sniffing, digest
        // computation, core binary storage)
        Blob blob = Blobs.createBlob(is);
        blob.setFilename(filename);

        dm.setPropertyValue(blobPropertyName, (Serializable) blob);

        getDocumentManager().saveDocument(dm);
        // autoversioning see https://jira.nuxeo.org/browse/NXP-5849 for more
        // details
        String versioningPolicy = Framework.getProperty(LIVED_AUTOVERSIONING_PROP);
        if (doAutoMinorIncrement(versioningPolicy, dm)) {
            if (dm.isCheckedOut()) {
                dm.checkIn(VersioningOption.MINOR, "Live edit (UploadFileRestlet) autoversioning");
            }
        }

        getDocumentManager().save();
    }

    /**
     * According to the policy, decide to auto minor increment or not
     *
     * @param policy
     * @return return true if the the version should be minor increment
     */
    protected boolean doAutoMinorIncrement(String policy, DocumentModel doc) {
        if (POLICY_MINOR_INCR.equals(policy)) {
            return true;
        }
        return false;

    }
}
