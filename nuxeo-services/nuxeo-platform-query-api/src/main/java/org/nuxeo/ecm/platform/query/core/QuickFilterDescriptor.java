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
import org.nuxeo.ecm.platform.query.api.QuickFilterDefinition;

/**
 * Descriptor for the quick filter used by page providers
 *
 * @author Funsho David
 * @since 8.4
 */
@XObject(value = "quickFilters")
public class QuickFilterDescriptor implements QuickFilterDefinition {

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
