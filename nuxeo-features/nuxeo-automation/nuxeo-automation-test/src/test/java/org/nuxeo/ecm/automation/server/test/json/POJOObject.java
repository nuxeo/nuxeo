/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ogrisel
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;

/**
 *
 * A simple POJO class that can be mapped as a json datastructure by jackson.
 */
public class POJOObject {

    String textContent = "";

    List<String> items = new ArrayList<String>();

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
        return String.format("%s(textContent=\"%s\", items=[\"%s\"])",
                getClass().getSimpleName(), textContent,
                StringUtils.join(items, "\", \""));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result
                + ((textContent == null) ? 0 : textContent.hashCode());
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
