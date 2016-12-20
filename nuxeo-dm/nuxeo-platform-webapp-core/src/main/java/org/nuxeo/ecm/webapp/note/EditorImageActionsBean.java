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

import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component implementing actions related to inserting an image in a Note document.
 * <p>
 * The uploaded image is stored in the <code>files</code> schema of the document.
 * <p>
 * After uploading an image, the REST URL for this image can be retrieve through the appropriate method.
 * <p>
 * The search method retrieves only the Picture document of the repository.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("editorImageActions")
@Scope(CONVERSATION)
public class EditorImageActionsBean extends InputController implements EditorImageActions, Serializable {

    private static final String FILES_SCHEMA = "files";

    private static final List<Map<String, String>> SIZES;

    /** @since 5.9.5 */
    private static final String PP_SEARCH_MEDIA_BY_TITLE = "search_media_by_title";

    /** @since 5.9.5 */
    private static final String PP_SEARCH_MEDIA_ALL = "search_media_all";

    static {
        SIZES = new ArrayList<Map<String, String>>();
        Map<String, String> m = new HashMap<String, String>();
        m.put("label", "label.imageUpload.originalSize");
        m.put("value", "OriginalJpeg");
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

    private Part uploadedImage;

    /**
     * @deprecated since 7.1
     */
    @Deprecated
    private String uploadedImageName;

    private String imageUrl;

    private boolean isImageUploaded = false;

    private List<DocumentModel> resultDocuments;

    private List<DocumentModel> resultVideos;

    private boolean hasSearchResults = false;

    private boolean hasSearchVideosResults = false;

    private String searchKeywords;

    private String selectedSize = "OriginalJpeg";

    @Override
    public String getSelectedTab() {
        if (selectedTab != null) {
            oldSelectedTab = selectedTab;
        } else if (oldSelectedTab == null) {
            oldSelectedTab = "UPLOAD";
        }
        return oldSelectedTab;
    }

    @Override
    public String getUrlForImage() {
        isImageUploaded = false;
        return imageUrl;
    }

    @Override
    public boolean getIsImageUploaded() {
        return isImageUploaded;
    }

    @Override
    public void setUploadedImage(Part uploadedImage) {
        this.uploadedImage = uploadedImage;
    }

    @Override
    public Part getUploadedImage() {
        return uploadedImage;
    }

    @Override
    public String getUploadedImageName() {
        return uploadedImageName;
    }

    @Override
    public void setUploadedImageName(String uploadedImageName) {
        this.uploadedImageName = uploadedImageName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String uploadImage() {
        if (uploadedImage == null) {
            return null;
        }
        DocumentModel doc = navigationContext.getCurrentDocument();
        List<Map<String, Object>> filesList = (List<Map<String, Object>>) doc.getProperty("files", "files");
        int fileIndex = filesList == null ? 0 : filesList.size();
        Map<String, Object> props = new HashMap<String, Object>();
        Blob blob;
        try {
            blob = FileUtils.createBlob(uploadedImage);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        props.put("file", blob);
        ListDiff listDiff = new ListDiff();
        listDiff.add(props);
        doc.setProperty("files", "files", listDiff);
        documentManager.saveDocument(doc);
        documentManager.save();
        imageUrl = DocumentModelFunctions.complexFileUrl("downloadFile", doc, fileIndex, blob.getFilename());
        isImageUploaded = true;
        return "editor_image_upload";
    }

    @Override
    public boolean getInCreationMode() {
        DocumentModel doc = navigationContext.getChangeableDocument();
        if (doc == null || doc.getRef() != null) {
            // if changeableDocument is null or has an existing ref, assume we
            // are not in creation and use the currentDocument instead
            doc = navigationContext.getCurrentDocument();
        }
        if (doc == null) {
            return false;
        }
        if (doc.getId() == null) {
            return true;
        } else {
            return !doc.hasSchema(FILES_SCHEMA);
        }
    }

    @Override
    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    @Override
    public boolean getHasSearchVideosResults() {
        return hasSearchVideosResults;
    }

    @Override
    public List<DocumentModel> getSearchImageResults() {
        return resultDocuments;
    }

    @Override
    public List<DocumentModel> getSearchVideosResults() {
        return resultVideos;
    }

    @Override
    public String getSearchKeywords() {
        return searchKeywords;
    }

    @Override
    public String searchImages() {
        // Init the list of results
        resultDocuments = null;
        // Search the images
        resultDocuments = searchMedia("Picture");
        hasSearchResults = !resultDocuments.isEmpty();
        log.debug("query result contains: " + resultDocuments.size() + " docs.");
        return "editor_image_upload";
    }

    /**
     * @since 5.9.5
     */
    @Override
    public String searchVideos() {
        // Init the list of results
        resultVideos = null;
        // Search the videos
        resultVideos = searchMedia("Video");
        hasSearchVideosResults = !resultVideos.isEmpty();

        log.debug("query result contains: " + resultVideos.size() + " videos.");
        return "editor_image_upload";
    }

    /**
     * Generic method to search a media.
     *
     * @param typeDocument The type of document to search.
     * @since 5.9.5
     */
    @SuppressWarnings("unchecked")
    private List<DocumentModel> searchMedia(String typeDocument) {
        log.debug("Entering searchDocuments with keywords: " + searchKeywords);

        // use page providers
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) documentManager);
        PageProvider<DocumentModel> pp = null;
        if (searchKeywords != null) {
            searchKeywords = searchKeywords.trim();
            if (searchKeywords.length() > 0) {
                if (!searchKeywords.equals("*")) {
                    // full text search
                    pp = (PageProvider<DocumentModel>) ppService.getPageProvider(PP_SEARCH_MEDIA_BY_TITLE, null, null,
                            null, props, new Object[] { typeDocument, searchKeywords });
                }
            }
        }

        // If the pageprovider is null, we search all medias for the specific type
        if (pp == null) {
            pp = (PageProvider<DocumentModel>) ppService.getPageProvider(PP_SEARCH_MEDIA_ALL, null, null, null, props,
                    new Object[] { typeDocument });
        }
        return pp.getCurrentPage();
    }

    @Override
    public void setSearchKeywords(final String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    @Override
    public List<Map<String, String>> getSizes() {
        return SIZES;
    }

    @Override
    public String getSelectedSize() {
        return selectedSize;
    }

    @Override
    public void setSelectedSize(final String selectedSize) {
        this.selectedSize = selectedSize;
    }

    @Override
    public String getImageProperty() {
        return selectedSize + ":content";
    }

    @Override
    public String getURLVideo(DocumentModel video, String type) {

        if (video == null || type == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) video.getPropertyValue("vid:transcodedVideos");
        int position = 0;
        for (Map<String, Serializable> prop : transcodedVideos) {
            if (type.equals(prop.get("name"))) {
                Blob content = (Blob) prop.get("content");
                String blobPropertyName = "vid:transcodedVideos/" + position + "/content";
                return DocumentModelFunctions.bigFileUrl(video, blobPropertyName, content.getFilename());
            }
            position++;
        }

        return null;
    }

}
