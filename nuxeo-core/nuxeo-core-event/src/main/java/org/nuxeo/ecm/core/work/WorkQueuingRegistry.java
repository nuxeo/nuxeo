/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
