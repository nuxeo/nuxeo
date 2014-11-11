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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.PictureView;

public interface MultiviewPicture {

    PictureView getView(String name) throws ClientException;

    void removeView(String name) throws ClientException;

    void addView(PictureView view) throws ClientException;

    PictureView[] getViews() throws ClientException;

    void removeAllView() throws ClientException;

    String getHeadline() throws ClientException;

    void setHeadline(String headline) throws ClientException;

    String getSubheadline() throws ClientException;

    void setSubheadline(String subheadline) throws ClientException;

    String getByline() throws ClientException;

    void setByline(String byline) throws ClientException;

    String getDateline() throws ClientException;

    void setDateline(String dateline) throws ClientException;

    String getSlugline() throws ClientException;

    void setSlugline(String slugline) throws ClientException;

    String getCredit() throws ClientException;

    void setCredit(String credit) throws ClientException;

    String getLanguage() throws ClientException;

    void setLanguage(String language) throws ClientException;

    String getSource() throws ClientException;

    void setSource(String source) throws ClientException;

    String getOrigin() throws ClientException;

    void setOrigin(String origin) throws ClientException;

    String getGenre() throws ClientException;

    void setGenre(String genre) throws ClientException;

    String getCaption() throws ClientException;

    void setCaption(String caption) throws ClientException;

    String getTypage() throws ClientException;

    void setTypage(String typage) throws ClientException;

}
