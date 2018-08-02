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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

/**
 * Directory ui descriptor
 *
 * @author Anahide Tchertchian
 */
@XObject("deleteConstraint")
public class DirectoryDeleteConstraintDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<? extends DirectoryDeleteConstraint> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<String, String>();

    public Class<? extends DirectoryDeleteConstraint> getKlass() {
        return klass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public DirectoryDeleteConstraint getDeleteConstraint() {
        try {
            DirectoryDeleteConstraint instance = klass.newInstance();
            if (properties != null) {
                instance.setProperties(properties);
            }
            return instance;
        } catch (InstantiationException e) {
            throw new DirectoryException(e);
        } catch (IllegalAccessException e) {
            throw new DirectoryException(e);
        }
    }

}
