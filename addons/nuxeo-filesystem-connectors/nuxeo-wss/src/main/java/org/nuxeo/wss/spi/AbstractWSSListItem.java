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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class AbstractWSSListItem implements WSSListItem {

    protected String icon;

    protected boolean isSiteItem = false;

    public boolean isFolderish() {
        return "folder".equals(getType());
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    protected static DateFormat getDateFormat() {
        // not thread-safe so don't use a static instance
        return new SimpleDateFormat("dd MMM yyyy HH:mm:ss -0000", Locale.US);
    }

    public String getCreatedTS() {
        return getDateFormat().format(getCreationDate());
    }

    public String getModifiedTS() {
        return getDateFormat().format(getModificationDate());
    }

    protected abstract Date getCheckoutDate();

    protected abstract Date getCheckoutExpiryDate();

    public String getCheckoutTS() {
        return getDateFormat().format(getCheckoutDate());
    }

    public String getCheckoutExpiryTS() {
        return getDateFormat().format(getCheckoutExpiryDate());
    }

    protected String getExtension() {
        String filename = getName();
        if (filename != null) {
            String parts[] = filename.split("\\.");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return null;
    }

    protected String getIconFromType() {
        if (isFolderish()) {
            return "folder.gif";
        } else {
            String ext = getExtension();
            if (ext != null) {
                if (ext.toLowerCase().equals("gif") || ext.toLowerCase().equals("jpg")
                        || ext.toLowerCase().equals("png") || ext.toLowerCase().equals("jpeg")
                        || ext.toLowerCase().equals("tif")) {
                    ext = "image";
                }
                return ext + ".gif";
            }
        }
        return "file.gif";
    }

    public String getIcon() {
        if (icon == null) {
            return getIconFromType();
        }
        return icon;
    }

    public String getSizeAsString() {
        return "" + getSize();
    }

    public String getDisplayName() {
        return getName();
    }

    public String getRelativeSubPath(String siteRootPath) {

        String subPath = getSubPath();
        if (siteRootPath != null && !"".equals(siteRootPath)) {
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }
            if (siteRootPath.startsWith("/")) {
                siteRootPath = siteRootPath.substring(1);
            }

            subPath = subPath.replace(siteRootPath, "");
        }
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }
        return subPath;
    }

    public String getRelativeFilePath(String siteRootPath) {
        return getRelativeSubPath(siteRootPath);
    }

    public boolean isSite() {
        return isSiteItem;
    }

    public boolean isCheckOut() {
        String lockingUser = getCheckoutUser();
        if (lockingUser != null) {
            return true;
        }
        return false;
    }

    public boolean canCheckOut(String userName) {
        String lockingUser = getCheckoutUser();
        if (lockingUser == null) {
            return true;
        } else {
            if (lockingUser.equals(userName)) {
                return true;
            }
        }
        return false;
    }

    public boolean canUnCheckOut(String userName) {
        String lockingUser = getCheckoutUser();
        if (lockingUser == null) {
            return false;
        } else {
            if (lockingUser.equals(userName)) {
                return true;
            }
        }
        return false;
    }

}
