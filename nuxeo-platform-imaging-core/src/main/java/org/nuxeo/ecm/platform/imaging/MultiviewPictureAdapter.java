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

package org.nuxeo.ecm.platform.imaging;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.platform.imaging.api.MultiviewPicture;
import org.nuxeo.ecm.platform.imaging.api.PictureView;
import org.nuxeo.ecm.platform.imaging.core.PictureViewImpl;

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
        Object o = null;
        PictureView view = new PictureViewImpl();
        view.setTitle((String) map.get(PictureView.FIELD_TITLE));
        view.setDescription((String) map.get(PictureView.FIELD_DESCRIPTION));
        view.setTag((String) map.get(PictureView.FIELD_TAG));
        o = map.get(PictureView.FIELD_WIDTH);
        if (o != null) {
            view.setWidth(((Number) o).intValue());
        }
        o = map.get(PictureView.FIELD_HEIGHT);
        if (o != null) {
            view.setHeight(((Number) o).intValue());
        }
        view.setFilename((String) map.get(PictureView.FIELD_FILENAME));
        view.setContent(map.get(PictureView.FIELD_CONTENT));
        return view;
    }

    @SuppressWarnings("unchecked")
    public MultiviewPictureAdapter(DocumentModel docModel) {
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

    public PictureView[] getViews() {
        Collection<PictureView> collection = views.values();
        return collection.toArray(new PictureView[collection.size()]);
    }

    public PictureView getView(String title) {
        return views.get(title);
    }

    public void removeView(String name) {
        views.remove(name);
        Vector<Map<String, Object>> v = new Vector<Map<String, Object>>();
        for (PictureView view : views.values()) {
            v.add(viewToMap(view));
        }
        docModel.setProperty("picture", "views", v.toArray());
    }

    public void addView(PictureView view) {
        views.put(view.getTitle(), view);
        List list = new ArrayList();
        for (PictureView pv : views.values()) {
            list.add(viewToMap(pv));
        }
        docModel.setProperty("picture", "views", list);
    }

    public void removeAllView() {
        docModel.setProperty("picture", "views", null);
        views.clear();
    }

    public String getHeadline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_HEADLINE);
    }

    public void setHeadline(String headline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_HEADLINE, headline);
    }

    public String getSubheadline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SUBHEADLINE);
    }

    public void setSubheadline(String subheadline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SUBHEADLINE, subheadline);
    }

    public String getByline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_BYLINE);
    }

    public void setByline(String byline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_BYLINE, byline);
    }

    public String getDateline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_DATELINE);
    }

    public void setDateline(String dateline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_DATELINE, dateline);
    }

    public String getSlugline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SLUGLINE);
    }

    public void setSlugline(String slugline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SLUGLINE, slugline);
    }

    public String getCredit() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CREDIT);
    }

    public void setCredit(String credit) {
        docModel.setProperty(SCHEMA_NAME, FIELD_CREDIT, credit);
    }

    public String getLanguage() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_LANGUAGE);
    }

    public void setLanguage(String language) {
        docModel.setProperty(SCHEMA_NAME, FIELD_LANGUAGE, language);
    }

    public String getSource() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SOURCE);
    }

    public void setSource(String source) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SOURCE, source);
    }

    public String getOrigin() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_ORIGIN);
    }

    public void setOrigin(String origin) {
        docModel.setProperty(SCHEMA_NAME, FIELD_ORIGIN, origin);
    }

    public String getGenre() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_GENRE);
    }

    public void setGenre(String genre) {
        docModel.setProperty(SCHEMA_NAME, FIELD_GENRE, genre);
    }

    public String getCaption() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CAPTION);
    }

    public void setCaption(String caption) {
        docModel.setProperty(SCHEMA_NAME, FIELD_CAPTION, caption);
    }

    public String getTypage() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_TYPAGE);
    }

    public void setTypage(String typage) {
        docModel.setProperty(SCHEMA_NAME, FIELD_TYPAGE, typage);
    }

}
