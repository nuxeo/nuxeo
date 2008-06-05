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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.note;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.file.UploadedFile;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Seam component implementing actions related to inserting an image in a Note document.
 * <p>
 * The uploaded image is stored in the <code>files</code> schema of the
 * document.
 * <p>
 * After uploading an image, the rest url for this image can be retrieve through
 * the appropriate method.
 * <p>
 * The search method retrieves only the Picture document of the repository.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
@Name("editorImageActions")
@Scope(CONVERSATION)
public class EditorImageActionsBean extends InputController implements
        EditorImageActions, Serializable {

    private static final String SEARCH_QUERY = "SELECT * FROM Document WHERE %s";

    private static final List<Map<String, String>> SIZES;

    static {
        SIZES = new ArrayList<Map<String,String>>();
        Map<String, String> m = new HashMap<String, String>();
        m.put("label", "label.imageUpload.originalSize");
        m.put("value", "Original");
        SIZES.add(m);
        m = new HashMap<String, String>();
        m.put("label", "label.imageUpload.mediumSize");
        m.put("value", "Medium");
        SIZES.add(m);
        m = new HashMap<String, String>();
        m.put("label", "label.imageUpload.thumbnailSize");
        m.put("value", "Thumbnail");
        SIZES.add(m);
    }

    private static final long serialVersionUID = 8716548847393060676L;

    private static final Log log = LogFactory.getLog(EditorImageActionsBean.class);

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @RequestParameter
    private String selectedTab;

    private String oldSelectedTab;

    private UploadedFile uploadedImage;

    private String imageUrl;

    private boolean isImageUploaded = false;

    private List<DocumentModel> resultDocuments;

    private boolean hasSearchResults = false;

    private String searchKeywords;

    private String selectedSize = "Original";

    public String getSelectedTab() {
        if (selectedTab != null) {
            oldSelectedTab = selectedTab;
        }
        return oldSelectedTab;
    }

    public String getUrlForImage() {
        isImageUploaded = false;
        return imageUrl;
    }

    public boolean getIsImageUploaded() {
        return isImageUploaded;
    }

    public void setUploadedImage(final UploadedFile uploadedImage) {
        this.uploadedImage = uploadedImage;
    }

    public UploadedFile getUploadedImage() {
        return uploadedImage;
    }

    @SuppressWarnings("unchecked")
    public String uploadImage() throws ClientException {
        if (uploadedImage == null) {
            return "";
        }
        final DocumentModel doc = navigationContext.getCurrentDocument();

        final List<Map<String, Object>> filesList = (List<Map<String, Object>>) doc.getProperty(
                "files", "files");
        final int fileIndex = filesList == null ? 0 : filesList.size();
        final String filename = uploadedImage.getFilename();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("filename", filename);
        try {
            props.put("file", new FileBlob(uploadedImage.getInputStream(),
                    uploadedImage.getContentType()));
        } catch (IOException e) {
            throw EJBExceptionHandler.wrapException(e);
        }
        final ListDiff listDiff = new ListDiff();
        listDiff.add(props);
        doc.setProperty("files", "files", listDiff);

        documentManager.saveDocument(doc);
        documentManager.save();

        imageUrl = DocumentModelFunctions.complexFileUrl("downloadFile", doc,
                fileIndex, filename);

        isImageUploaded = true;

        return "editor_image_upload";
    }

    public boolean getInCreationMode() {
        final DocumentModel doc = navigationContext.getCurrentDocument();
        return !"note".equalsIgnoreCase(doc.getType());
    }

    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    public List<DocumentModel> getSearchImageResults() {
        return resultDocuments;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public String searchImages() throws ClientException {
        log.debug("Entering searchDocuments with keywords: " + searchKeywords);

        resultDocuments = null;
        final List<String> constraints = new ArrayList<String>();
        if (searchKeywords != null) {
            searchKeywords = searchKeywords.trim();
            if (searchKeywords.length() > 0) {
                if (!searchKeywords.equals("*")) {
                    // full text search
                    constraints.add(String.format("ecm:fulltext LIKE '%s'",
                            searchKeywords));
                }
            }
        }
        constraints.add("ecm:primaryType = 'Picture'");

        final String query = String.format(SEARCH_QUERY, StringUtils.join(constraints, " AND "));
        final SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        final ComposedNXQuery composedQuery = new ComposedNXQueryImpl(nxqlQuery);
        final SearchService searchService = SearchServiceDelegate.getRemoteSearchService();

        try {
            final ResultSet queryResults = searchService.searchQuery(
                    composedQuery, 0, 100);
            if (queryResults != null) {
                final SearchPageProvider provider = new SearchPageProvider(
                        queryResults);
                resultDocuments = provider.getCurrentPage();
            }
            log.debug("FTQ query result contains: " + resultDocuments.size()
                    + " docs.");
            hasSearchResults = !resultDocuments.isEmpty();
        } catch (QueryException e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "label.search.service.wrong.query"));
            log.error("QueryException in search popup : " + e.getMessage());
        } catch (QueryParseException e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "label.search.service.wrong.query"));
            log.error("QueryParseException in search popup : " + e.getMessage());
        } catch (SearchException e) {
            throw EJBExceptionHandler.wrapException(e);
        }
        return "editor_image_upload";
    }

    public void setSearchKeywords(final String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public List<Map<String, String>> getSizes() {
        return SIZES;
    }

    public String getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(final String selectedSize) {
        this.selectedSize = selectedSize;
    }

    public String getImageProperty() {
        return selectedSize + ":content";
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

}
