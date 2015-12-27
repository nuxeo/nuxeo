/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Descriptor for sort info declaration.
 *
 * @author Anahide Tchertchian
 */
@XObject("sort")
public class SortInfoDescriptor {

    @XNode("@column")
    String column;

    @XNode("@ascending")
    boolean ascending = true;

    public String getColumn() {
        return column;
    }

    public boolean isAscending() {
        return ascending;
    }

    public SortInfo getSortInfo() {
        return new SortInfo(column, ascending);
    }

    /**
     * @since 5.6
     */
    @Override
    public SortInfoDescriptor clone() {
        SortInfoDescriptor clone = new SortInfoDescriptor();
        clone.column = column;
        clone.ascending = ascending;
        return clone;
    }
}
