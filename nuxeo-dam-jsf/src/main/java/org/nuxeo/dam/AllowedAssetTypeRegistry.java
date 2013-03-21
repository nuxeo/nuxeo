package org.nuxeo.dam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for activity verbs, handling merge of registered
 * {@link AllowedAssetTypeDescriptor} elements.
 *
 * @since 5.7
 */
public class AllowedAssetTypeRegistry extends
        ContributionFragmentRegistry<AllowedAssetTypeDescriptor> {

    protected Map<String, AllowedAssetTypeDescriptor> allowedAssetTypes = new HashMap<String, AllowedAssetTypeDescriptor>();

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
    public void contributionUpdated(String id,
            AllowedAssetTypeDescriptor contrib,
            AllowedAssetTypeDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            allowedAssetTypes.put(id, contrib);
        } else {
            allowedAssetTypes.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id,
            AllowedAssetTypeDescriptor origContrib) {
        allowedAssetTypes.remove(id);
    }

    @Override
    public AllowedAssetTypeDescriptor clone(AllowedAssetTypeDescriptor orig) {
        return new AllowedAssetTypeDescriptor(orig);
    }

    @Override
    public void merge(AllowedAssetTypeDescriptor src,
            AllowedAssetTypeDescriptor dst) {
        boolean enabled = src.isEnabled();
        if (enabled != dst.isEnabled()) {
            dst.setEnabled(enabled);
        }
    }

}
