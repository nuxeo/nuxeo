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

package org.nuxeo.ecm.directory.api.ui;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryDeleteConstraintDescriptor;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

/**
 * Directory ui descriptor
 *
 * @author Anahide Tchertchian
 */
@XObject("directory")
public class DirectoryUIDescriptor implements DirectoryUI {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@view")
    String view;

    @XNode("@layout")
    String layout;

    @XNode("@sortField")
    String sortField;

    @XNode("@enabled")
    Boolean enabled;

    @XNode("@readOnly")
    Boolean readOnly;

    @XNodeList(value = "deleteConstraint", type = ArrayList.class, componentType = DirectoryDeleteConstraintDescriptor.class)
    List<DirectoryDeleteConstraintDescriptor> deleteConstraints;

    public String getName() {
        return name;
    }

    public String getLayout() {
        return layout;
    }

    public String getView() {
        return view;
    }

    public String getSortField() {
        return sortField;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public List<DirectoryDeleteConstraint> getDeleteConstraints() {
        List<DirectoryDeleteConstraint> res = new ArrayList<DirectoryDeleteConstraint>();
        if (deleteConstraints != null) {
            for (DirectoryDeleteConstraintDescriptor deleteConstraintDescriptor : deleteConstraints) {
                res.add(deleteConstraintDescriptor.getDeleteConstraint());
            }
        }
        return res;
    }

}
