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

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
@Name("pictureManager")
@Scope(CONVERSATION)
public class PictureManagerBean implements PictureManager, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PictureManagerBean.class);

    protected static Boolean imageMagickAvailable;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @RequestParameter
    protected String fileFieldFullName;

    @In(required = true, create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected ResourcesAccessor resourcesAccessor;

    protected String fileurlPicture;

    protected String filename;

    protected Blob fileContent;

    protected Integer index;

    protected String cropCoords;

    protected ArrayList<Map<String, Object>> selectItems;

    @Override
    @Create
    public void initialize() {
        log.debug("Initializing...");
        index = 0;
    }

    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getFileurlPicture() {
        ArrayList<Map<String, Object>> views = (ArrayList) getCurrentDocument().getProperty("picture", "views");
        return views.get(index).get("title") + ":content";
    }

    @Override
    public void setFileurlPicture(String fileurlPicture) {
        this.fileurlPicture = fileurlPicture;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void initSelectItems() {
        selectItems = new ArrayList<Map<String, Object>>();
        DocumentModel doc = getCurrentDocument();
        ArrayList<Map<String, Object>> views = (ArrayList) doc.getProperty("picture", "views");
        for (int i = 0; i < views.size(); i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("title", views.get(i).get("title"));
            map.put("idx", i);
            selectItems.add(map);
        }
    }

    @Override
    public ArrayList getSelectItems() {
        if (selectItems == null) {
            initSelectItems();
            return selectItems;
        } else {
            return selectItems;
        }
    }

    @Override
    public void setSelectItems(ArrayList selectItems) {
        this.selectItems = selectItems;
    }

    @Override
    public String rotate90left() throws IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(-90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    @Override
    public String rotate90right() throws IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    @Override
    public String crop() throws IOException {
        if (cropCoords != null && !cropCoords.equals("")) {
            DocumentModel doc = getCurrentDocument();
            PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
            picture.doCrop(cropCoords);
            documentManager.saveDocument(doc);
            documentManager.save();
            navigationContext.setCurrentDocument(doc);
        }
        return null;
    }

    @Override
    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED })
    @BypassInterceptors
    public void resetFields() {
        filename = "";
        fileContent = null;
        selectItems = null;
        index = 0;
        cropCoords = null;
    }

    @WebRemote
    public String remoteDownload(String patternName, String docID, String blobPropertyName, String filename)
            {
        IdRef docref = new IdRef(docID);
        DocumentModel doc = documentManager.getDocument(docref);
        return DocumentModelFunctions.fileUrl(patternName, doc, blobPropertyName, filename);
    }

    @WebRemote
    public static String urlPopup(String url) {
        return RestHelper.addCurrentConversationParameters(url);
    }

    @Override
    public void download(DocumentView docView) {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            // fix for NXP-1799
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(docLoc.getServerName());
                navigationContext.setCurrentServerLocation(loc);
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc == null) {
                return;
            }
            String path = docView.getParameter(DocumentFileCodec.FILE_PROPERTY_PATH_KEY);
            String[] propertyPath = path.split(":");
            String title = null;
            String field = null;
            Property datamodel = null;
            if (propertyPath.length == 2) {
                title = propertyPath[0];
                field = propertyPath[1];
                datamodel = doc.getProperty("picture:views");
            } else if (propertyPath.length == 3) {
                String schema = propertyPath[0];
                title = propertyPath[1];
                field = propertyPath[2];
                datamodel = doc.getProperty(schema + ":" + "views");
            }
            Property view = null;
            for (Property property : datamodel) {
                if (property.get("title").getValue().equals(title)) {
                    view = property;
                }
            }

            if (view == null) {
                for (Property property : datamodel) {
                    if (property.get("title").getValue().equals("Thumbnail")) {
                        view = property;
                    }
                }
            }
            if (view == null) {
                return;
            }
            Blob blob = (Blob) view.getValue(field);
            String filename = (String) view.getValue("filename");
            // download
            ComponentUtils.download(doc, path, blob, filename, "picture");
        }
    }

    @Override
    @Destroy
    public void destroy() {
        log.debug("Removing Seam action listener...");
        fileurlPicture = null;
        filename = null;
        fileContent = null;
        index = null;
        selectItems = null;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public Blob getFileContent() {
        return fileContent;
    }

    @Override
    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String getCropCoords() {
        return cropCoords;
    }

    @Override
    public void setCropCoords(String cropCoords) {
        this.cropCoords = cropCoords;
    }

    public Boolean isImageMagickAvailable() {
        if (imageMagickAvailable == null) {
            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CommandAvailability ca = cles.getCommandAvailability("cropAndResize");
            imageMagickAvailable = ca.isAvailable();
        }
        return imageMagickAvailable;
    }
}
