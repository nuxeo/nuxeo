/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.configuration;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
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

    public AnnotationDefinition(String uri, String name, String icon, String type) {
        this.uri = uri;
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    public AnnotationDefinition(String uri, String name, String icon, String type, String listIcon, String createIcon,
            Boolean inMenu, Map<String, String[]> fields) {
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
