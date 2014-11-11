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

package org.nuxeo.ecm.platform.picture.api;

public interface PictureView {

    String FIELD_TITLE = "title";

    String FIELD_DESCRIPTION = "description";

    String FIELD_TAG = "tag";

    String FIELD_WIDTH = "width";

    String FIELD_HEIGHT = "height";

    String FIELD_FILENAME = "filename";

    String FIELD_CONTENT = "content";

    String getTitle();

    void setTitle(String title);

    String getTag();

    void setTag(String tag);

    String getDescription();

    void setDescription(String description);

    int getHeight();

    void setHeight(int height);

    int getWidth();

    void setWidth(int width);

    String getFilename();

    void setFilename(String filename);

    Object getContent();

    void setContent(Object content);

}
