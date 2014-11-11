/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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

    //TODO
//    @XContent
//    public void setValueFromContent(String value) {
//        this.value = PropertyDecoder.decode(type, value);
//    }

    public Object getValue() {
        return value;
    }

    // Not used.
    //public void setValue(Object value) {
    //    this.value = value;
    //}

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
