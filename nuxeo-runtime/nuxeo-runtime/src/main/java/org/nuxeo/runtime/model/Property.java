/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.impl.PropertyDecoder;

/**
 * @author Bogdan Stefanescu
 */
@XObject(value = "property", order = { "@name", "@type" })
public class Property implements Serializable {

    private static final long serialVersionUID = -2661183859962287565L;

    @XNode("@name")
    private String name;

    @XNode("@type")
    private String type = "string";

    private Serializable value;

    @XNode("@value")
    private void setStringValue(String value) {
        this.value = PropertyDecoder.decode(type, value);
    }

    // TODO
    // @XContent
    // public void setValueFromContent(String value) {
    // this.value = PropertyDecoder.decode(type, value);
    // }

    public Object getValue() {
        return value;
    }

    // Not used.
    // public void setValue(Object value) {
    // this.value = value;
    // }

    // Not used.
    public String getType() {
        return type;
    }

    // Not used.
    public String getName() {
        return name;
    }

    // Not used.
    public String getString() {
        return value.toString();
    }

    // Not used.
    public Integer getInteger() {
        return (Integer) value;
    }

    // Not used.
    public Boolean getBoolean() {
        return (Boolean) value;
    }

    // Not used.
    @SuppressWarnings("unchecked")
    public List<String> getList() {
        return (List<String>) value;
    }

}
