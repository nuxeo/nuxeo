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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Provide Creation Book related actions.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @deprecated since 5.9.6. See NXP-15370.
 */
@Deprecated
public interface PictureBookManager {

    /**
     * Sets the title. This is the title of the PictureBook as defined in the
     * Dublincore schema
     *
     * @param title a String holding the title
     */
    void setTitle(String title);

    /**
     * Gets the title. This is the title of the PictureBook as defined in the
     * Dublincore schema.
     *
     * @return a String holding the title
     */
    String getTitle();

    /**
     * Gets the views. The views are Map that contains different information
     * about each type of picture you might want. It has the 3 default following
     * views: Original View, Medium View, used for the slideShow Thumbnail View,
     * used For the PictureBook.
     *
     * @return an ArrayList of Map<String, Object> holding the views
     */
    ArrayList<Map<String, Object>> getViews();

    /**
     * Sets the views. The views are Map that contains different information
     * about each type of picture you might want. It has the 3 default following
     * views: Original View, Medium View, used for the slideShow Thumbnail View,
     * used For the PictureBook
     *
     * @param views an ArrayList of Map<String, Object> holding the views
     */
    void setViews(ArrayList<Map<String, Object>> views);

    /**
     * Sets the viewtitle. viewtitle is the property title of a view. For
     * example the default title For the Original view is 'Original'.
     *
     * @param viewtitle a String holding the title of a view
     */
    void setViewtitle(String viewtitle);

    /**
     * Gets the viewtitle. viewtitle is the property title of a view. For
     * example the default title For the Original view is 'Original'
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
     * Sets the maxsize. maxsize is a property of a view. Maximum size of the
     * longest side of a picture. It is used to create the file of a view.
     *
     * @param maxsize an Integer holding the maximum size
     */
    void setMaxsize(Integer maxsize);

    /**
     * Gets the maxsize. maxsize is a property of a view. Maximum size of the
     * longest side of a picture. It is used to create the file of a view.
     *
     * @return an Integer holding the maximum size
     */
    Integer getMaxsize();

    /**
     * Adds the current view to the views ArrayList using viewtitle,
     * description, tag and maxsize.
     */
    void addView();

    /**
     * Creates and saves a Picture Book DocumentModel using views, timeinterval,
     * description and title.
     */
    String createPictureBook() throws Exception;

    void reset() throws ClientException;

    void initialize() throws Exception;

    String downloadSelectedBook() throws ClientException, IOException;

    String downloadAll() throws ClientException, IOException;

    List<SelectItem> getSelectItems() throws ClientException;

    void setSelectItems(List<SelectItem> selectItems);

    String[] getSelectedViews();

    void setSelectedViews(String[] selectedViews);

}
