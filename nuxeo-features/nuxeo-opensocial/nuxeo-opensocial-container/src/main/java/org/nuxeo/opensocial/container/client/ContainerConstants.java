/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client;

import com.google.gwt.i18n.client.Dictionary;

/**
 * @author Stéphane Fourrier
 */
public class ContainerConstants {

    Dictionary dic = Dictionary.getDictionary("opensocial_messages");

    public String windowTitle() {
        return dic.get("windowTitle");
    }

    public String containerSizeTitle() {
        return dic.get("containerSizeTitle");
    }

    public String sizeInPixel() {
        return dic.get("sizeInPixel");
    }

    public String customSize() {
        return dic.get("customSize");
    }

    public String unknown() {
        return dic.get("unknown");
    }

    public String sideBarTitle() {
        return dic.get("sideBarTitle");
    }

    public String customContentTitle() {
        return dic.get("customContentTitle");
    }

    public String addRow() {
        return dic.get("addRow");
    }

    public String headerNFooterTitle() {
        return dic.get("headerNFooterTitle");
    }

    public String enableHeader() {
        return dic.get("enableHeader");
    }

    public String enableFooter() {
        return dic.get("enableFooter");
    }

    public String showCodeTitle() {
        return dic.get("showCodeTitle");
    }

    public String showCode() {
        return dic.get("showCode");
    }

    public String closeTitle() {
        return dic.get("closeTitle");
    }

    public String close() {
        return dic.get("close");
    }

    public String preferences() {
        return dic.get("preferences");
    }

    public String title() {
        return dic.get("title");
    }

    public String headerColor() {
        return dic.get("headerColor");
    }

    public String titleColor() {
        return dic.get("titleColor");
    }

    public String borderColor() {
        return dic.get("borderColor");
    }

    public String save() {
        return dic.get("save");
    }

    public String choose() {
        return dic.get("choose");
    }

    public String createdBy() {
        return dic.get("createdBy");
    }

    public String folderSelection() {
        return dic.get("folderSelection");
    }

    public String cantDeleteLastZoneError() {
        return dic.get("cantDeleteLastZoneError");
    }

    public String fold() {
        return dic.get("fold");
    }

    public String unfold() {
        return dic.get("unfold");
    }

    public String configure() {
        return dic.get("configure");
    }

    public String maximize() {
        return dic.get("maximize");
    }

    public String minimize() {
        return dic.get("minimize");
    }
}
