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

package org.nuxeo.ecm.platform.picture.api.adapters;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_VIEWS_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
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

    public static final String FIELD_INFO = "info";

    public static final String SCHEMA_NAME = "picture";

    final DocumentModel docModel;

    final Map<String, PictureView> views = new HashMap<>();

    public static Map<String, Object> viewToMap(PictureView view) {
        Map<String, Object> map = new HashMap<>();
        map.put(PictureView.FIELD_TITLE, view.getTitle());
        map.put(PictureView.FIELD_DESCRIPTION, view.getDescription());
        map.put(PictureView.FIELD_TAG, view.getTag());
        map.put(PictureView.FIELD_HEIGHT, view.getHeight());
        map.put(PictureView.FIELD_WIDTH, view.getWidth());
        map.put(PictureView.FIELD_FILENAME, view.getFilename());
        Blob blob = view.getBlob();
        if (blob != null) {
            map.put(PictureView.FIELD_CONTENT, blob);
        }
        map.put(FIELD_INFO, view.getImageInfo().toMap());
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
        view.setBlob(blob);
        view.setImageInfo(ImageInfo.fromMap((Map<String, Serializable>) map.get(FIELD_INFO)));
        return view;
    }

    @SuppressWarnings("unchecked")
    public MultiviewPictureAdapter(DocumentModel docModel) {
        this.docModel = docModel;

        List<Map<String, Object>> list = (List<Map<String, Object>>) docModel.getPropertyValue(PICTURE_VIEWS_PROPERTY);
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
    public void removeView(String name) {
        views.remove(name);
        List<Map<String, Object>> v = new ArrayList<>();
        for (PictureView view : views.values()) {
            v.add(viewToMap(view));
        }
        docModel.setPropertyValue(PICTURE_VIEWS_PROPERTY, v.toArray());
    }

    @Override
    public void addView(PictureView view) {
        views.put(view.getTitle(), view);
        List<Map<String, Object>> list = new ArrayList<>();
        for (PictureView pv : views.values()) {
            list.add(viewToMap(pv));
        }
        docModel.setPropertyValue(PICTURE_VIEWS_PROPERTY, (Serializable) list);
    }

    @Override
    public void removeAllView() {
        docModel.setPropertyValue(PICTURE_VIEWS_PROPERTY, null);
        views.clear();
    }

    @Override
    public String getHeadline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_HEADLINE);
    }

    @Override
    public void setHeadline(String headline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_HEADLINE, headline);
    }

    @Override
    public String getSubheadline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SUBHEADLINE);
    }

    @Override
    public void setSubheadline(String subheadline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SUBHEADLINE, subheadline);
    }

    @Override
    public String getByline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_BYLINE);
    }

    @Override
    public void setByline(String byline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_BYLINE, byline);
    }

    @Override
    public String getDateline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_DATELINE);
    }

    @Override
    public void setDateline(String dateline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_DATELINE, dateline);
    }

    @Override
    public String getSlugline() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SLUGLINE);
    }

    @Override
    public void setSlugline(String slugline) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SLUGLINE, slugline);
    }

    @Override
    public String getCredit() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CREDIT);
    }

    @Override
    public void setCredit(String credit) {
        docModel.setProperty(SCHEMA_NAME, FIELD_CREDIT, credit);
    }

    @Override
    public String getLanguage() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_LANGUAGE);
    }

    @Override
    public void setLanguage(String language) {
        docModel.setProperty(SCHEMA_NAME, FIELD_LANGUAGE, language);
    }

    @Override
    public String getSource() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_SOURCE);
    }

    @Override
    public void setSource(String source) {
        docModel.setProperty(SCHEMA_NAME, FIELD_SOURCE, source);
    }

    @Override
    public String getOrigin() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_ORIGIN);
    }

    @Override
    public void setOrigin(String origin) {
        docModel.setProperty(SCHEMA_NAME, FIELD_ORIGIN, origin);
    }

    @Override
    public String getGenre() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_GENRE);
    }

    @Override
    public void setGenre(String genre) {
        docModel.setProperty(SCHEMA_NAME, FIELD_GENRE, genre);
    }

    @Override
    public String getCaption() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_CAPTION);
    }

    @Override
    public void setCaption(String caption) {
        docModel.setProperty(SCHEMA_NAME, FIELD_CAPTION, caption);
    }

    @Override
    public String getTypage() {
        return (String) docModel.getProperty(SCHEMA_NAME, FIELD_TYPAGE);
    }

    @Override
    public void setTypage(String typage) {
        docModel.setProperty(SCHEMA_NAME, FIELD_TYPAGE, typage);
    }

}
