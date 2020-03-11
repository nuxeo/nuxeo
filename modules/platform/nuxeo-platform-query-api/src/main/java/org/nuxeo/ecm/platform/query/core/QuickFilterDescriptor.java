/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.QuickFilter;

/**
 * Descriptor for the quick filter used by page providers
 *
 * @author Funsho David
 * @since 8.4
 */
@XObject(value = "quickFilters")
public class QuickFilterDescriptor implements QuickFilter {

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clause == null) ? 0 : clause.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((sortInfos == null) ? 0 : sortInfos.hashCode());
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
        QuickFilterDescriptor other = (QuickFilterDescriptor) obj;
        if (clause == null) {
            if (other.clause != null)
                return false;
        } else if (!clause.equals(other.clause))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (sortInfos == null) {
            if (other.sortInfos != null)
                return false;
        } else if (!sortInfos.equals(other.sortInfos))
            return false;
        return true;
    }

    @XNode("@name")
    protected String name;

    @XNode("clause")
    protected String clause;

    @XNodeList(value = "sort", type = ArrayList.class, componentType = SortInfoDescriptor.class)
    protected List<SortInfoDescriptor> sortInfos;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClause() {
        return clause;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        List<SortInfo> infos = new ArrayList<>();
        for (SortInfoDescriptor sortInfoDesc : sortInfos) {
            infos.add(sortInfoDesc.getSortInfo());
        }
        return infos;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setClause(String clause) {
        this.clause = clause;
    }

    @Override
    public QuickFilterDescriptor clone() {
        QuickFilterDescriptor clone = new QuickFilterDescriptor();
        clone.name = getName();
        clone.clause = getClause();
        if (sortInfos != null) {
            clone.sortInfos = new ArrayList<>();
            for (SortInfoDescriptor sortInfo : sortInfos) {
                clone.sortInfos.add(sortInfo);
            }
        }
        return clone;
    }
}
