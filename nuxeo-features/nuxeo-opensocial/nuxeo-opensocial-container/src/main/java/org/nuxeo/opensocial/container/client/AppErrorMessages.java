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
public class AppErrorMessages {

    Dictionary dic = Dictionary.getDictionary("opensocial_messages");

    public String unitIsNotEmpty() {
        return dic.get("unitIsNotEmpty");
    }

    public String zoneIsNotEmpty() {
        return dic.get("zoneIsNotEmpty");
    }

    public String noZoneCreated() {
        return dic.get("noZoneCreated");
    }

    public String cannotLoadLayout() {
        return dic.get("cannotLoadLayout");
    }

    public String cannotReachServer() {
        return dic.get("cannotReachServer");
    }

    public String applicationNotCorrectlySet() {
        return dic.get("applicationNotCorrectlySet");
    }

    public String cannotUpdateLayout() {
        return dic.get("cannotUpdateLayout");
    }

    public String cannotUpdateFooter() {
        return dic.get("cannotUpdateFooter");
    }

    public String cannotCreateZone() {
        return dic.get("cannotCreateZone");
    }

    public String cannotUpdateZone() {
        return dic.get("cannotUpdateZone");
    }

    public String cannotUpdateSideBar() {
        return dic.get("cannotUpdateSideBar");
    }

    public String cannotUpdateHeader() {
        return dic.get("cannotUpdateHeader");
    }

    public String cannotDeleteZone() {
        return dic.get("cannotDeleteZone");
    }

    public String cannotCreateWebContent() {
        return dic.get("cannotCreateWebContent");
    }

    public String cannotLoadWebContents() {
        return dic.get("cannotLoadWebContents");
    }

    public String cannotUpdateAllWebContents() {
        return dic.get("cannotUpdateAllWebContents");
    }

    public String cannotUpdateWebContent() {
        return dic.get("cannotUpdateWebContent");
    }

    public String cannotDeleteWebContent() {
        return dic.get("cannotDeleteWebContent");
    }

    public String cannotLoadContainerBuilder() {
        return dic.get("cannotLoadContainerBuilder");
    }

    public String cannotAddExternalWebContent(String type) {
        return dic.get("cannotAddExternalWebContent") + type;
    }

    public String cannotFindWebContent() {
        return dic.get("cannotFindWebContent");
    }

    public String preferenceDoesNotExist(String name) {
        return dic.get("preferenceDoesNotExist") + name;
    }
}
