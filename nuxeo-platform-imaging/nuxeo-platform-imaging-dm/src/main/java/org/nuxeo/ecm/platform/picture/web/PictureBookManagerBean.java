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

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBlobHolder;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Provide Picture Book related Actions.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @deprecated since 5.9.6. See NXP-15370.
 */
@Name("pictureBookManager")
@Scope(CONVERSATION)
@Deprecated
public class PictureBookManagerBean extends InputController implements
        PictureBookManager, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PictureBookManagerBean.class);

    @In(create = true)
    protected CoreSession documentManager;

    protected Integer maxsize;

    protected ArrayList<Map<String, Object>> views;

    protected String title;

    protected String viewtitle;

    protected String tag;

    protected String description;

    protected List<SelectItem> selectItems;

    protected String[] selectedViews = { "Original" };

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    protected static final int BUFFER = 2048;

    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    @Override
    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        initViews();
    }

    protected void initViews() {
        // Sets the default views original, thumbnail and medium.
        views = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", "Medium");
        map.put("maxsize", AbstractPictureAdapter.MEDIUM_SIZE);
        map.put("tag", "medium");
        map.put("description", "MediumSize Picture");
        views.add(map);
        map = new HashMap<String, Object>();
        map.put("title", "Original");
        map.put("maxsize", null);
        map.put("tag", "original");
        map.put("description", "Original Picture");
        views.add(map);
        map = new HashMap<String, Object>();
        map.put("title", "Thumbnail");
        map.put("maxsize", AbstractPictureAdapter.THUMB_SIZE);
        map.put("tag", "thumbnail");
        map.put("description", "ThumbnailSize Picture");
        views.add(map);
        map = new HashMap<String, Object>();
        map.put("title", "OriginalJpeg");
        map.put("maxsize", null);
        map.put("tag", "originalJpeg");
        map.put("description", "Original Picture in JPEG format");
        views.add(map);
    }

    @Destroy
    @BypassInterceptors
    public void destroy() {
        title = null;
        viewtitle = null;
        maxsize = null;
        tag = null;
        description = null;
        views = null;
        log.debug("Destroy");
    }

    @Override
    public String createPictureBook() throws Exception {
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel doc = navigationContext.getChangeableDocument();

        String parentPath;
        if (getCurrentDocument() == null) {
            // creating item at the root
            parentPath = documentManager.getRootDocument().getPathAsString();
        } else {
            parentPath = navigationContext.getCurrentDocument().getPathAsString();
        }

        doc.setProperty("picturebook", "picturetemplates", views);

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                documentManager.getDocument(new PathRef(parentPath)));
        doc.setPathInfo(parentPath, pss.generatePathSegment(doc));
        doc = documentManager.createDocument(doc);
        documentManager.saveDocument(doc);
        documentManager.save();

        return navigationContext.getActionResult(doc, UserAction.AFTER_CREATE);
    }

    @Override
    public void addView() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", viewtitle);
        map.put("maxsize", maxsize);
        map.put("tag", tag);
        map.put("description", description);
        views.add(map);
    }

    @Override
    @Observer({ EventNames.DOCUMENT_SELECTION_CHANGED })
    @BypassInterceptors
    public void reset() throws ClientException {
        title = null;
        maxsize = null;
        viewtitle = null;
        tag = null;
        description = null;
        selectItems = null;
        selectedViews = new String[] { "Original" };
        initViews();
    }

    @Override
    public String downloadSelectedBook() throws ClientException, IOException {
        List<DocumentModel> list = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        return createZip(list);
    }

    @Override
    public String downloadAll() throws ClientException, IOException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc != null) {
            List<DocumentModel> list = documentManager.getChildren(currentDoc.getRef());
            return createZip(list);
        }
        return null;
    }

    protected boolean isEmptyFolder(DocumentModel doc) throws ClientException {
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            BlobHolder bh = docChild.getAdapter(BlobHolder.class);
            if (docChild.isFolder()) {
                return isEmptyFolder(docChild);
            } else if (bh != null) {
                return false;
            }
        }
        return true;
    }

    protected String formatFileName(String filename, String count) {
        StringBuilder sb = new StringBuilder();
        CharSequence name = filename.subSequence(0, filename.lastIndexOf("."));
        CharSequence extension = filename.subSequence(
                filename.lastIndexOf("."), filename.length());
        sb.append(name).append(count).append(extension);
        return sb.toString();
    }

    protected void addBlobHolderToZip(String path, ZipOutputStream out,
            byte[] data, PictureBlobHolder bh) throws IOException,
            ClientException {
        List<Blob> blobs;
        if (selectedViews != null) {
            blobs = bh.getBlobs(selectedViews);
        } else {
            blobs = bh.getBlobs();
        }
        for (Blob content : blobs) {
            String fileName = content.getFilename();
            if (content != null) {
                // Workaround to deal with duplicate file names.
                int tryCount = 0;
                while (true) {
                    try {
                        if (tryCount == 0) {
                            ZipUtils._zip(path + fileName, content.getStream(),
                                    out);
                        } else {
                            ZipUtils._zip(
                                    path
                                            + formatFileName(fileName, "("
                                                    + tryCount + ")"),
                                    content.getStream(), out);
                        }
                        break;
                    } catch (ZipException e) {
                        tryCount++;
                    }
                }
            }
        }
    }

    protected void addFolderToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data) throws ClientException, IOException {

        String title = (String) doc.getProperty("dublincore", "title");
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {

            // NXP-2334 : skip deleted docs
            if (docChild.getCurrentLifeCycleState().equals("delete")) {
                continue;
            }

            BlobHolder bh = docChild.getAdapter(BlobHolder.class);
            if (docChild.isFolder() && !isEmptyFolder(docChild)) {
                addFolderToZip(path + title + "/", out, docChild, data);
            } else if (bh != null) {
                addBlobHolderToZip(path + title + "/", out, data,
                        (PictureBlobHolder) bh);
            }
        }
    }

    protected String createZip(List<DocumentModel> documents)
            throws IOException, ClientException {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        BufferedOutputStream buff = new BufferedOutputStream(
                response.getOutputStream());
        ZipOutputStream out = new ZipOutputStream(buff);
        out.setMethod(ZipOutputStream.DEFLATED);
        out.setLevel(9);
        byte[] data = new byte[BUFFER];
        for (DocumentModel doc : documents) {

            // first check if DM is attached to the core
            if (doc.getSessionId() == null) {
                // refetch the doc from the core
                doc = documentManager.getDocument(doc.getRef());
            }

            // NXP-2334 : skip deleted docs
            if (doc.getCurrentLifeCycleState().equals("delete")) {
                continue;
            }

            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (doc.isFolder() && !isEmptyFolder(doc)) {
                addFolderToZip("", out, doc, data);
            } else if (bh != null) {
                addBlobHolderToZip("", out, data, (PictureBlobHolder) bh);
            }
        }
        try {
            out.close();
        } catch (ZipException e) {
            // empty zip file, do nothing
            setFacesMessage("label.clipboard.emptyDocuments");
            return null;
        }
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + "clipboard.zip" + "\";");
        response.setContentType("application/gzip");
        response.flushBuffer();
        context.responseComplete();
        return null;
    }

    protected void initSelectItems() throws ClientException {
        DocumentModel doc = getCurrentDocument();
        List<Map<String, Object>> views = (List) doc.getProperty("picturebook",
                "picturetemplates");
        selectItems = new ArrayList<SelectItem>(views.size());
        String label;
        SelectItem selectItem;
        for (Map<String, Object> map : views) {
            label = (String) map.get("title");
            selectItem = new SelectItem(label, label);
            selectItems.add(selectItem);
        }
    }

    @Override
    public List<SelectItem> getSelectItems() throws ClientException {
        if (selectItems == null) {
            initSelectItems();
            return selectItems;
        } else {
            return selectItems;
        }
    }

    @Override
    public void setSelectItems(List<SelectItem> selectItems) {
        this.selectItems = selectItems;
    }

    @Override
    public String[] getSelectedViews() {
        return selectedViews;
    }

    @Override
    public void setSelectedViews(String[] selectedViews) {
        this.selectedViews = selectedViews;
    }

    @Override
    public Integer getMaxsize() {
        return maxsize;
    }

    @Override
    public void setMaxsize(Integer maxsize) {
        this.maxsize = maxsize;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getViewtitle() {
        return viewtitle;
    }

    @Override
    public void setViewtitle(String viewtitle) {
        this.viewtitle = viewtitle;
    }

    @Override
    public ArrayList<Map<String, Object>> getViews() {
        return views;
    }

    @Override
    public void setViews(ArrayList<Map<String, Object>> views) {
        this.views = views;
    }

}
