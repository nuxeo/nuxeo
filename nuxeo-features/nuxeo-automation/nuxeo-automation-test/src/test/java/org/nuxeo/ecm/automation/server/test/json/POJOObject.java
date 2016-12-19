/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ogrisel
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;

/**
 * A simple POJO class that can be mapped as a json datastructure by jackson.
 */
public class POJOObject {

    String textContent = "";

    List<String> items = new ArrayList<>();

    public POJOObject() {
        // I am a well behaved POJO suitable for automated serialization by
        // jackson
    }

    public POJOObject(String textContent, List<String> items) {
        this.textContent = textContent;
        this.items = items;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return String.format("%s(textContent=\"%s\", items=[\"%s\"])", getClass().getSimpleName(), textContent,
                String.join("\", \"", items));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + ((textContent == null) ? 0 : textContent.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        POJOObject other = (POJOObject) obj;
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        if (textContent == null) {
            if (other.textContent != null)
                return false;
        } else if (!textContent.equals(other.textContent))
            return false;
        return true;
    }

}
