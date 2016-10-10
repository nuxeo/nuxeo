/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.ecm.core.work;

import org.nuxeo.ecm.core.work.api.WorkQueuingDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 *
 *
 * @since 8.3
 */
public class WorkQueuingRegistry extends ContributionFragmentRegistry<WorkQueuingDescriptor> {

    Class<? extends WorkQueuing> klass = MemoryWorkQueuing.class;

    @Override
    public String getContributionId(WorkQueuingDescriptor contrib) {
        return "singleton";
    }

    @Override
    public void contributionUpdated(String id, WorkQueuingDescriptor contrib, WorkQueuingDescriptor newOrigContrib) {
        klass = contrib.klass;
    }

    @Override
    public void contributionRemoved(String id, WorkQueuingDescriptor origContrib) {
        klass = origContrib.klass;
    }

    @Override
    public WorkQueuingDescriptor clone(WorkQueuingDescriptor orig) {
        WorkQueuingDescriptor other = new WorkQueuingDescriptor();
        other.klass = orig.klass;
        return other;
    }

    @Override
    public void merge(WorkQueuingDescriptor src, WorkQueuingDescriptor dst) {
        dst.klass = src.klass;
    }


}
