/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.versioning;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * @since 9.1
 */
@XObject("policy")
public class VersioningPolicyDescriptor implements Serializable, Comparable<VersioningPolicyDescriptor> {

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    protected String id;

    @XNode("@order")
    protected Integer order;

    @XNode("@increment")
    protected VersioningOption increment;

    @XNode("@beforeUpdate")
    protected boolean beforeUpdate;

    @XNode("initialState")
    protected InitialStateDescriptor initialState;

    @XNodeList(value = "filter-id", componentType = String.class, type = ArrayList.class)
    protected List<String> filterIds = new ArrayList<>();

    public String getId() {
        return id;
    }

    public int getOrder() {
        return defaultIfNull(order, 20);
    }

    public VersioningOption getIncrement() {
        return increment;
    }

    public boolean isBeforeUpdate() {
        return beforeUpdate;
    }

    public InitialStateDescriptor getInitialState() {
        return initialState;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    public void merge(VersioningPolicyDescriptor other) {
        id = defaultIfNull(other.id, id);
        order = defaultIfNull(other.order, order);
        increment = defaultIfNull(other.increment, increment);
        initialState = defaultIfNull(other.initialState, initialState);
        filterIds.addAll(other.filterIds);
    }

    public int compareTo(VersioningPolicyDescriptor versioningPolicyDescriptor) {
        return Integer.compare(getOrder(), versioningPolicyDescriptor.getOrder());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + id + ", increment=" + increment + ", beforeUpdate=" + beforeUpdate
                + ')';
    }

}
