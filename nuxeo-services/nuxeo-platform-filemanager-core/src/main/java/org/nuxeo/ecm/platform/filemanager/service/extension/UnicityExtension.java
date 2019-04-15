/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("unicitySettings")
public class UnicityExtension implements Descriptor, Serializable {

    private static final long serialVersionUID = 7764225025169187266L;

    public static final List<String> DEFAULT_FIELDS = new ArrayList<>();

    @XNode("algo")
    protected String algo;

    @XNode("enabled")
    protected Boolean enabled;

    @XNode("computeDigest")
    protected Boolean computeDigest = Boolean.FALSE;

    @XNodeList(value = "field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields = DEFAULT_FIELDS;

    public String getAlgo() {
        return algo;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public List<String> getFields() {
        return fields;
    }

    public Boolean getComputeDigest() {
        if (Boolean.TRUE.equals(enabled)) {
            return enabled;
        }
        return computeDigest;
    }

    @Override
    public String getId() {
        return toString();
    }
}
