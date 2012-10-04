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

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;

public class MultiviewPictureAdapter implements MultiviewPicture {

    public static final String FIELD_HEADLINE = "headline";

    public static final String FIELD_SUBHEADLINE = "subheadline";

    public static final String FIELD_BYLINE = "byline";

    public static final String FIELD_DATELINE = "dateline";

    public static final String FIELD_SLUGLINE = "slugline";

    public static final String FIELD_CREDIT = "credit";

    public static final String FIELD_LANGUAGE = "language";

    public static final String FIELD_SOURCE = "source";

    public static final String FIELD_ORIGIN = "origin";

    public static final String FIELD_GENRE = "genre";

    public static final String FIELD_CAPTION = "caption";

    public static final String FIELD_TYPAGE = "typage";

    public static final String SCHEMA_NAME = "picture";

    final DocumentModel docModel;

    final Map<String, PictureView> views = new HashMap<String, PictureView>();

    public static Map<String, Object> viewToMap(PictureView view) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PictureView.FIELD_TITLE, view.getTitle());
        map.put(PictureView.FIELD_DESCRIPTION, view.getDescription());
        map.put(PictureView.FIELD_TAG, view.getTag());
        map.put(PictureView.FIELD_HEIGHT, view.getHeight());
        map.put(PictureView.FIELD_WIDTH, view.getWidth());
        map.put(PictureView.FIELD_FILENAME, view.getFilename());
        Object o = view.getContent();
        Blob blob = null;
        if (o instanceof File) {
            blob = new FileBlob((File) o, "application/octet-stream");
        } else if (o instanceof InputStream) {
            blob = new InputStreamBlob((InputStream) o,
                    "application/octet-stream");
        } else if (o instanceof Blob) {
            blob = (Blob) o;
        }
        if (blob != null) {
            map.put(PictureView.FIELD_CONTENT, blob);
        }
        return map;
    }

    public static PictureView mapToView(Map<String, Object> map) {
        PictureView view = new PictureViewImpl();
        view.setTitle((String) map.get(PictureView.FIELD_TITLE));
        view.setDescription((String) map.get(PictureView.FIELD_DESCRIPTION));
        view.setTag((String) map.get(PictureView.FIELD_TAG));
        Object o = map.get(PictureView.FIELD_WIDTH);
        if (o != null) {
            view.setWidth(((Number) o).intValue());
        }
        o = map.get(PictureView.FIELD_HEIGHT);
        if (o != null) {
            view.setHeight(((Number) o).intValue());
        }
        view.setFilename((String) map.get(PictureView.FIELD_FILENAME));
        Blob blob = (Blob) map.get(PictureView.FIELD_CONTENT);
        view.setContent(blob);
        view.setBlob(blob);
        return view;
    }

    @SuppressWarnings("unchecked")
    public MultiviewPictureAdapter(DocumentModel docModel)
            throws ClientException {
        this.docModel = docModel;
        Object o = docModel.getProperty("picture", "views");

        List<Map<String, Object>> list = (List<Map<String, Object>>) docModel.getProperty(
                "picture", "views");
        if (list != null) {
            for (Map<String, Object> map : list) {
                PictureView view = mapToView(map);
                views.put(view.getTitle(), view);
            }
        }
    }

    @Override
    public PictureView[] getViews() {
        Collection<PictureView> collection = views.values();
        return collection.toArray(new PictureView[collection.size()]);
    }

    @Override
    public PictureView getView(String title) {
        return views.get(title);
    }

    @Override
    public void removeView(String name) throws ClientException {
        views.remove(name);
        List<Map<String, Object>> v = new ArrayList<Map<String, Object>>();
        for (PictureView view : views.values()) {
            v.add(viewToMap(view));
        }
        docModel.setProperty("picture", "views", v.toArray());
    }

    @Override
    public void addView(PictureView view) throws ClientException {
        views.put(view.getTitle(), view);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (PictureView pv : views.values()) {
            list.add(viewToMap(pv));
        }
        docModel.setProperty("picture", "views", list);
    }

    @Override
    public void removeAllView() throws ClientException {
        docModel.setProperty("picture", "views", null);
        views.clear();
    }

    @Override
    public String getHeadline() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_HEADLINE);
    }

    @Override
    public void setHeadline(String headline) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_HEADLINE, headline);
    }

    @Override
    public String getSubheadline() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SUBHEADLINE);
    }

    @Override
    public void setSubheadline(String subheadline) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_SUBHEADLINE, subheadline);
    }

    @Override
    public String getByline() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_BYLINE);
    }

    @Override
    public void setByline(String byline) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_BYLINE, byline);
    }

    @Override
    public String getDateline() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_DATELINE);
    }

    @Override
    public void setDateline(String dateline) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_DATELINE, dateline);
    }

    @Override
    public String getSlugline() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SLUGLINE);
    }

    @Override
    public void setSlugline(String slugline) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_SLUGLINE, slugline);
    }

    @Override
    public String getCredit() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CREDIT);
    }

    @Override
    public void setCredit(String credit) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_CREDIT, credit);
    }

    @Override
    public String getLanguage() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_LANGUAGE);
    }

    @Override
    public void setLanguage(String language) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_LANGUAGE, language);
    }

    @Override
    public String getSource() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SOURCE);
    }

    @Override
    public void setSource(String source) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_SOURCE, source);
    }

    @Override
    public String getOrigin() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_ORIGIN);
    }

    @Override
    public void setOrigin(String origin) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_ORIGIN, origin);
    }

    @Override
    public String getGenre() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_GENRE);
    }

    @Override
    public void setGenre(String genre) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_GENRE, genre);
    }

    @Override
    public String getCaption() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CAPTION);
    }

    @Override
    public void setCaption(String caption) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_CAPTION, caption);
    }

    @Override
    public String getTypage() throws ClientException {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_TYPAGE);
    }

    @Override
    public void setTypage(String typage) throws ClientException {
        docModel.setProperty(SCHEMA_NAME, FIELD_TYPAGE, typage);
    }

}
