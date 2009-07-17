/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.configuration;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationDefinition implements IsSerializable {

    private String uri;

    private String name;

    private String icon;

    private String type;

    private String listIcon;

    private String createIcon;

    private Boolean inMenu;

    private Map<String, String[]> fields;

    public AnnotationDefinition() {

    }

    public AnnotationDefinition(String uri, String name, String icon,
            String type) {
        this.uri = uri;
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    public AnnotationDefinition(String uri, String name, String icon,
            String type, String listIcon, String createIcon, Boolean inMenu,
            Map<String, String[]> fields) {
        this.uri = uri;
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.listIcon = listIcon;
        this.createIcon = createIcon;
        this.inMenu = inMenu;
        this.fields = fields;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setListIcon(String listIcon) {
        this.listIcon = listIcon;
    }

    public String getListIcon() {
        return listIcon;
    }

    public void setCreateIcon(String createIcon) {
        this.createIcon = createIcon;
    }

    public String getCreateIcon() {
        return createIcon;
    }

    public void setInMenu(Boolean inMenu) {
        this.inMenu = inMenu;
    }

    public Boolean isInMenu() {
        return inMenu;
    }

    public void setFields(Map<String, String[]> fields) {
        this.fields = fields;
    }

    public Map<String, String[]> getFields() {
        return fields;
    }

}
