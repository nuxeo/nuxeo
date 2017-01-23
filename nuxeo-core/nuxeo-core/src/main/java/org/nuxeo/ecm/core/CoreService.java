/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service used to register version removal policies.
 */
public class CoreService extends DefaultComponent {

    private static final String VERSION_REMOVAL_POLICY_XP = "versionRemovalPolicy";

    private static final String ORPHAN_VERSION_REMOVAL_FILTER_XP = "orphanVersionRemovalFilter";

    private static final String VERSION_PROPERTIES_XP = "versionProperties";

    protected Map<CoreServicePolicyDescriptor, VersionRemovalPolicy> versionRemovalPolicies = new LinkedHashMap<>();

    protected Map<CoreServiceOrphanVersionRemovalFilterDescriptor, OrphanVersionRemovalFilter> orphanVersionRemovalFilters = new LinkedHashMap<>();

    protected List<String> versionProperties = new ArrayList<>(1);

    protected ComponentContext context;

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (VERSION_REMOVAL_POLICY_XP.equals(point)) {
            registerVersionRemovalPolicy((CoreServicePolicyDescriptor) contrib);
        } else if (ORPHAN_VERSION_REMOVAL_FILTER_XP.equals(point)) {
            registerOrphanVersionRemovalFilter((CoreServiceOrphanVersionRemovalFilterDescriptor) contrib);
        } else if (VERSION_PROPERTIES_XP.equals(point)) {
            registerVersionProperty((CoreServiceVersionPropertyDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String point, ComponentInstance contributor) {
        if (VERSION_REMOVAL_POLICY_XP.equals(point)) {
            unregisterVersionRemovalPolicy((CoreServicePolicyDescriptor) contrib);
        } else if (ORPHAN_VERSION_REMOVAL_FILTER_XP.equals(point)) {
            unregisterOrphanVersionRemovalFilter((CoreServiceOrphanVersionRemovalFilterDescriptor) contrib);
        } else if (VERSION_PROPERTIES_XP.equals(point)) {
            unregisterVersionProperty((CoreServiceVersionPropertyDescriptor) contrib);
        }
    }

    protected void registerVersionRemovalPolicy(CoreServicePolicyDescriptor contrib) {
        // log.warn("Extension point );
        String klass = contrib.getKlass();
        try {
            VersionRemovalPolicy policy = (VersionRemovalPolicy) context.getRuntimeContext().loadClass(klass).newInstance();
            versionRemovalPolicies.put(contrib, policy);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + VERSION_REMOVAL_POLICY_XP + ": " + klass, e);
        }
    }

    protected void unregisterVersionRemovalPolicy(CoreServicePolicyDescriptor contrib) {
        versionRemovalPolicies.remove(contrib);
    }

    protected void registerOrphanVersionRemovalFilter(CoreServiceOrphanVersionRemovalFilterDescriptor contrib) {
        String klass = contrib.getKlass();
        try {
            OrphanVersionRemovalFilter filter = (OrphanVersionRemovalFilter) context.getRuntimeContext().loadClass(
                    klass).newInstance();
            orphanVersionRemovalFilters.put(contrib, filter);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + ORPHAN_VERSION_REMOVAL_FILTER_XP + ": " + klass, e);
        }
    }

    protected void unregisterOrphanVersionRemovalFilter(CoreServiceOrphanVersionRemovalFilterDescriptor contrib) {
        orphanVersionRemovalFilters.remove(contrib);
    }

    protected void registerVersionProperty(CoreServiceVersionPropertyDescriptor descr) {
        versionProperties.add(descr.property);
    }

    protected void unregisterVersionProperty(CoreServiceVersionPropertyDescriptor descr) {
        versionProperties.remove(descr.property);
    }

    /**
     * Checks if we should use the default orphan version removal policy, which is faster than pluggable ones.
     * <p>
     * Pluggable orphan version removal policies will be deprecated in the future.
     *
     * @return {@code true} if the default orphan version removal policy should be used
     * @since 9.1
     */
    public boolean useDefaultOrphanVersionRemovalPolicy() {
        return versionRemovalPolicies.isEmpty();
    }

    /**
     * Returns the list of additional properties containing version ids which should block orphan version removal. Used
     * only when {@link #useDefaultOrphanVersionRemovalPolicy} is {@code true}.
     *
     * @return a list of property names
     * @since 9.1
     */
    public List<String> getVersionsProperties() {
        return versionProperties;
    }

    /**
     * Removes the orphan versions for a working document that is about to be removed.
     * <p>
     * Only called when a pluggable policy is used.
     *
     * @param session the current session
     * @param doc the document that is about to be removed
     * @param coreSession the current core session
     * @since 9.1
     */
    public void removeOrphanVersions(Document doc, CoreSession coreSession) {
        getVersionRemovalPolicy().removeVersions(doc.getSession(), doc, coreSession);
    }

    /**
     * Gets the last version removal policy registered.
     */
    public VersionRemovalPolicy getVersionRemovalPolicy() {
        if (versionRemovalPolicies.isEmpty()) {
            return DefaultVersionRemovalPolicy.INSTANCE;
        } else {
            VersionRemovalPolicy versionRemovalPolicy = null;
            for (VersionRemovalPolicy policy : versionRemovalPolicies.values()) {
                versionRemovalPolicy = policy;
            }
            return versionRemovalPolicy;
        }
    }

    /** Gets all the orphan version removal filters registered. */
    public Collection<OrphanVersionRemovalFilter> getOrphanVersionRemovalFilters() {
        return orphanVersionRemovalFilters.values();
    }

}
