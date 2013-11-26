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
 *     qlamerand
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.gwt.client.annotea.RDFConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AnnotationFilter implements IsSerializable {
    private String name;

    private String icon;

    private String type;

    private String author;

    private Map<String, String> fields;

    private List<String> parameters = new ArrayList<String>();

    public AnnotationFilter() {
    }

    public AnnotationFilter(String name, String icon, String type,
            String author, Map<String, String> fields) {
        this.name = name;
        this.icon = icon;

        if ("".equals(type)) {
            parameters.add(RDFConstant.R_TYPE);
        } else {
            this.type = type;
        }

        if ("".equals(author)) {
            parameters.add(RDFConstant.D_CREATOR);
        } else {
            this.author = author;
        }

        this.fields = fields;
        if (fields != null) {
            List<String> fieldNames = new ArrayList<String>(fields.keySet());
            for (String fieldName : fieldNames) {
                if ("".equals(fields.get(fieldName))) {
                    parameters.add(fieldName);
                    fields.remove(fieldName);
                }
            }
        }
    }

    public boolean accept(Annotation annotation) {
        boolean accept = true;

        if (type != null) {
            accept &= type.equals(annotation.getShortType());
        }

        if (author != null) {
            accept &= author.equals(annotation.getAuthor());
        }

        if (fields != null) {
            for (String name : fields.keySet()) {
                accept &= fields.get(name).equals(
                        annotation.getFields().get(name));
            }
        }

        return accept;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getField(String name) {
        return fields.get(name);
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public void setField(String name, String value) {
        fields.put(name, value);
    }

    public void removeField(String name) {
        fields.remove(name);
    }

    public List<String> getParameters() {
        return parameters;
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

}
