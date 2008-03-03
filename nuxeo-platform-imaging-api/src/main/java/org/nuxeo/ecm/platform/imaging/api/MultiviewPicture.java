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

package org.nuxeo.ecm.platform.imaging.api;


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
