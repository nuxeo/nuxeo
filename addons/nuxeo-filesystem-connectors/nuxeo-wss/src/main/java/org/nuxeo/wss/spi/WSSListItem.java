/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi;

import java.io.InputStream;
import java.util.Date;

import org.nuxeo.wss.WSSException;

public interface WSSListItem {

    String getAuthor();

    String getSubPath();

    String getRelativeSubPath(String siteRootPath);

    String getRelativeFilePath(String siteRootPath);

    String getDescription();

    void setDescription(String description);

    InputStream getStream();

    void setStream(InputStream stream, String fileName) throws WSSException;

    String getName();

    String getEtag();

    String getType();

    boolean isFolderish();

    String getIcon();

    void setIcon(String icon);

    String getModifiedTS();

    String getCreatedTS();

    int getSize();

    String getSizeAsString();

    void checkOut(String userName) throws WSSException;

    void uncheckOut(String userName) throws WSSException;

    String getLastModificator();

    String getCheckoutTS();

    String getCheckoutExpiryTS();

    String getCheckoutUser();

    String getDisplayName();

    boolean isSite();

    Date getCreationDate();

    Date getModificationDate();

    boolean canCheckOut(String userName);

    boolean canUnCheckOut(String userName);

    boolean isCheckOut();

}
