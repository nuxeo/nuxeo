/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since 6.0
 */
@XObject(value = "header")
public class NuxeoHeaderDescriptor implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled = true;

    protected String value;

    public String getValue() {
        return value;
    }

    @XContent
    public void setValue(String value) {
        if (value != null) {
            this.value = value.trim();
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public NuxeoHeaderDescriptor clone() throws CloneNotSupportedException {
        NuxeoHeaderDescriptor d = new NuxeoHeaderDescriptor();
        d.name = name;
        d.enabled = enabled;
        d.value = value;
        return d;
    }

    public void merge(NuxeoHeaderDescriptor source) {
        enabled = source.enabled;
        if (source.value != null) {
            value = source.value;
        }
    }

}
