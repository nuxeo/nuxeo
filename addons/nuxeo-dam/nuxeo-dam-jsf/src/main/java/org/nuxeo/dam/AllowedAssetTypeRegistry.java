/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.dam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for activity verbs, handling merge of registered {@link AllowedAssetTypeDescriptor} elements.
 *
 * @since 5.7
 */
public class AllowedAssetTypeRegistry extends ContributionFragmentRegistry<AllowedAssetTypeDescriptor> {

    protected Map<String, AllowedAssetTypeDescriptor> allowedAssetTypes = new LinkedHashMap<>();

    public List<String> getAllowedAssetTypes() {
        List<String> types = new ArrayList<String>();
        for (AllowedAssetTypeDescriptor allowedAssetType : allowedAssetTypes.values()) {
            types.add(allowedAssetType.getName());
        }
        return types;
    }

    @Override
    public String getContributionId(AllowedAssetTypeDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, AllowedAssetTypeDescriptor contrib,
            AllowedAssetTypeDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            allowedAssetTypes.put(id, contrib);
        } else {
            allowedAssetTypes.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, AllowedAssetTypeDescriptor origContrib) {
        allowedAssetTypes.remove(id);
    }

    @Override
    public AllowedAssetTypeDescriptor clone(AllowedAssetTypeDescriptor orig) {
        return new AllowedAssetTypeDescriptor(orig);
    }

    @Override
    public void merge(AllowedAssetTypeDescriptor src, AllowedAssetTypeDescriptor dst) {
        boolean enabled = src.isEnabled();
        if (enabled != dst.isEnabled()) {
            dst.setEnabled(enabled);
        }
    }

}
