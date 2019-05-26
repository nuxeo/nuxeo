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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;


/**
 * Provide Creation Book related actions.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @deprecated since 6.0. See NXP-15370.
 */
@Deprecated
public interface PictureBookManager {

    /**
     * Sets the title. This is the title of the PictureBook as defined in the Dublincore schema
     *
     * @param title a String holding the title
     */
    void setTitle(String title);

    /**
     * Gets the title. This is the title of the PictureBook as defined in the Dublincore schema.
     *
     * @return a String holding the title
     */
    String getTitle();

    /**
     * Gets the views. The views are Map that contains different information about each type of picture you might want.
     * It has the 3 default following views: Original View, Medium View, used for the slideShow Thumbnail View, used For
     * the PictureBook.
     *
     * @return an ArrayList of Map<String, Object> holding the views
     */
    ArrayList<Map<String, Object>> getViews();

    /**
     * Sets the views. The views are Map that contains different information about each type of picture you might want.
     * It has the 3 default following views: Original View, Medium View, used for the slideShow Thumbnail View, used For
     * the PictureBook
     *
     * @param views an ArrayList of Map<String, Object> holding the views
     */
    void setViews(ArrayList<Map<String, Object>> views);

    /**
     * Sets the viewtitle. viewtitle is the property title of a view. For example the default title For the Original
     * view is 'Original'.
     *
     * @param viewtitle a String holding the title of a view
     */
    void setViewtitle(String viewtitle);

    /**
     * Gets the viewtitle. viewtitle is the property title of a view. For example the default title For the Original
     * view is 'Original'
     *
     * @return a String holding the title of a view.
     */
    String getViewtitle();

    /**
     * Sets the description. description is a property of a view.
     *
     * @param description a String holding the description
     */
    void setDescription(String description);

    /**
     * Gets the description. description is a property of a view.
     *
     * @return a String holding the description
     */
    String getDescription();

    /**
     * Sets the tag. tag is a property of a view.
     *
     * @param tag a String holding the tag
     */
    void setTag(String tag);

    /**
     * Gets the tag. tag is a property of a view.
     *
     * @return a String holding the description
     */
    String getTag();

    /**
     * Sets the maxsize. maxsize is a property of a view. Maximum size of the longest side of a picture. It is used to
     * create the file of a view.
     *
     * @param maxsize an Integer holding the maximum size
     */
    void setMaxsize(Integer maxsize);

    /**
     * Gets the maxsize. maxsize is a property of a view. Maximum size of the longest side of a picture. It is used to
     * create the file of a view.
     *
     * @return an Integer holding the maximum size
     */
    Integer getMaxsize();

    /**
     * Adds the current view to the views ArrayList using viewtitle, description, tag and maxsize.
     */
    void addView();

    /**
     * Creates and saves a Picture Book DocumentModel using views, timeinterval, description and title.
     */
    String createPictureBook();

    void reset();

    void initialize();

    String downloadSelectedBook() throws IOException;

    String downloadAll() throws IOException;

    List<SelectItem> getSelectItems();

    void setSelectItems(List<SelectItem> selectItems);

    String[] getSelectedViews();

    void setSelectedViews(String[] selectedViews);

}
