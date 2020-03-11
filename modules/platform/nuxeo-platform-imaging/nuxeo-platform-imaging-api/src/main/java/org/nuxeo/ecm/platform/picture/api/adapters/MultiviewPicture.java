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

import org.nuxeo.ecm.platform.picture.api.PictureView;

public interface MultiviewPicture {

    PictureView getView(String name);

    void removeView(String name);

    void addView(PictureView view);

    PictureView[] getViews();

    void removeAllView();

    String getHeadline();

    void setHeadline(String headline);

    String getSubheadline();

    void setSubheadline(String subheadline);

    String getByline();

    void setByline(String byline);

    String getDateline();

    void setDateline(String dateline);

    String getSlugline();

    void setSlugline(String slugline);

    String getCredit();

    void setCredit(String credit);

    String getLanguage();

    void setLanguage(String language);

    String getSource();

    void setSource(String source);

    String getOrigin();

    void setOrigin(String origin);

    String getGenre();

    void setGenre(String genre);

    String getCaption();

    void setCaption(String caption);

    String getTypage();

    void setTypage(String typage);

}
