/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Versioning service component and implementation.
 */
public class VersioningComponent extends DefaultComponent implements VersioningService {

    private static final Log log = LogFactory.getLog(VersioningComponent.class);

    public static final String VERSIONING_SERVICE_XP = "versioningService";

    public static final String VERSIONING_RULE_XP = "versioningRules";

    public static final String VERSIONING_POLICY_XP = "policies";

    public static final String VERSIONING_FILTER_XP = "filters";

    protected static final StandardVersioningService STANDARD_VERSIONING_SERVICE = new StandardVersioningService();

    protected Map<VersioningServiceDescriptor, VersioningService> versioningServices = new LinkedHashMap<>();

    protected VersioningRuleRegistry versioningRulesRegistry = new VersioningRuleRegistry();

    protected VersioningPolicyRegistry versioningPoliciesRegistry = new VersioningPolicyRegistry();

    protected VersioningFilterRegistry versioningFiltersRegistry = new VersioningFilterRegistry();

    protected static class VersioningPolicyRegistry extends SimpleContributionRegistry<VersioningPolicyDescriptor> {

        @Override
        public String getContributionId(VersioningPolicyDescriptor contrib) {
            return contrib.getId();
        }

        public Map<String, VersioningPolicyDescriptor> getVersioningPolicyDescriptors() {
            return currentContribs.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

    }

    protected static class VersioningFilterRegistry extends SimpleContributionRegistry<VersioningFilterDescriptor> {

        @Override
        public String getContributionId(VersioningFilterDescriptor contrib) {
            return contrib.getId();
        }

        public Map<String, VersioningFilterDescriptor> getVersioningFilterDescriptors() {
            return currentContribs;
        }

    }

    protected static class VersioningRuleRegistry extends SimpleContributionRegistry<VersioningRuleDescriptor> {

        @Override
        public String getContributionId(VersioningRuleDescriptor contrib) {
            return contrib.getTypeName();
        }

        @Override
        public VersioningRuleDescriptor clone(VersioningRuleDescriptor orig) {
            return new VersioningRuleDescriptor(orig);
        }

        @Override
        public void merge(VersioningRuleDescriptor src, VersioningRuleDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        @Override
        public void contributionUpdated(String id, VersioningRuleDescriptor contrib,
                VersioningRuleDescriptor newOrigContrib) {
            if (contrib.isEnabled()) {
                currentContribs.put(id, contrib);
            } else {
                currentContribs.remove(id);
            }
        }

        public void clear() {
            currentContribs.clear();
        }

        public Map<String, VersioningRuleDescriptor> getVersioningRuleDescriptors() {
            return currentContribs;
        }
    }

    protected Deque<DefaultVersioningRuleDescriptor> defaultVersioningRuleList = new ArrayDeque<>();

    // public for tests
    public VersioningService service = STANDARD_VERSIONING_SERVICE;

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
        switch (point) {
        case VERSIONING_SERVICE_XP:
            registerVersioningService((VersioningServiceDescriptor) contrib);
            break;
        case VERSIONING_RULE_XP:
            if (contrib instanceof VersioningRuleDescriptor) {
                registerVersioningRule((VersioningRuleDescriptor) contrib);
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                registerDefaultVersioningRule((DefaultVersioningRuleDescriptor) contrib);
            } else {
                throw new RuntimeException("Unknown contribution to " + point + ": " + contrib.getClass());
            }
            break;
        case VERSIONING_POLICY_XP:
            registerVersioningPolicy((VersioningPolicyDescriptor) contrib);
            break;
        case VERSIONING_FILTER_XP:
            registerVersioningFilter((VersioningFilterDescriptor) contrib);
            break;
        default:
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String point, ComponentInstance contributor) {
        switch (point) {
        case VERSIONING_SERVICE_XP:
            unregisterVersioningService((VersioningServiceDescriptor) contrib);
            break;
        case VERSIONING_RULE_XP:
            if (contrib instanceof VersioningRuleDescriptor) {
                unregisterVersioningRule((VersioningRuleDescriptor) contrib);
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                unregisterDefaultVersioningRule((DefaultVersioningRuleDescriptor) contrib);
            }
            break;
        case VERSIONING_POLICY_XP:
            unregisterVersioningPolicy((VersioningPolicyDescriptor) contrib);
            break;
        case VERSIONING_FILTER_XP:
            unregisterVersioningFilter((VersioningFilterDescriptor) contrib);
            break;
        default:
            break;
        }
    }

    protected void registerVersioningService(VersioningServiceDescriptor contrib) {
        String klass = contrib.className;
        try {
            VersioningService vs = (VersioningService) context.getRuntimeContext().loadClass(klass).newInstance();
            versioningServices.put(contrib, vs);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate: " + klass, e);
        }
        log.info("Registered versioning service: " + klass);
        recompute();
    }

    protected void unregisterVersioningService(VersioningServiceDescriptor contrib) {
        versioningServices.remove(contrib);
        log.info("Unregistered versioning service: " + contrib.className);
        recompute();
    }

    protected void registerVersioningRule(VersioningRuleDescriptor contrib) {
        versioningRulesRegistry.addContribution(contrib);
        log.info("Registered versioning rule: " + contrib.getTypeName());
        recompute();
    }

    protected void unregisterVersioningRule(VersioningRuleDescriptor contrib) {
        versioningRulesRegistry.removeContribution(contrib);
        log.info("Unregistered versioning rule: " + contrib.getTypeName());
        recompute();
    }

    protected void registerDefaultVersioningRule(DefaultVersioningRuleDescriptor contrib) {
        // could use a linked set instead, but given the size a linked list is enough
        defaultVersioningRuleList.add(contrib);
        recompute();
    }

    protected void unregisterDefaultVersioningRule(DefaultVersioningRuleDescriptor contrib) {
        defaultVersioningRuleList.remove(contrib);
        recompute();
    }

    protected void registerVersioningPolicy(VersioningPolicyDescriptor contrib) {
        versioningPoliciesRegistry.addContribution(contrib);
        log.info("Registered versioning policy: " + contrib.getId());
        recompute();
    }

    protected void unregisterVersioningPolicy(VersioningPolicyDescriptor contrib) {
        versioningPoliciesRegistry.removeContribution(contrib);
        log.info("Unregistered versioning policy: " + contrib.getId());
        recompute();
    }

    protected void registerVersioningFilter(VersioningFilterDescriptor contrib) {
        versioningFiltersRegistry.addContribution(contrib);
        log.info("Registered versioning filter: " + contrib.getId());
        recompute();
    }

    protected void unregisterVersioningFilter(VersioningFilterDescriptor contrib) {
        versioningFiltersRegistry.removeContribution(contrib);
        log.info("Unregistered versioning filter: " + contrib.getId());
        recompute();
    }

    protected void recompute() {
        VersioningService versioningService = STANDARD_VERSIONING_SERVICE;
        for (VersioningService vs : versioningServices.values()) {
            versioningService = vs;
        }
        if (versioningService instanceof ExtendableVersioningService) {
            ExtendableVersioningService vs = (ExtendableVersioningService) versioningService;
            vs.setVersioningRules(getVersioningRules());
            vs.setDefaultVersioningRule(getDefaultVersioningRule());
            vs.setVersioningPolicies(getVersioningPolicies());
            vs.setVersioningFilters(getVersioningFilters());
        }
        this.service = versioningService;
    }

    protected Map<String, VersioningRuleDescriptor> getVersioningRules() {
        return versioningRulesRegistry.getVersioningRuleDescriptors();
    }

    protected DefaultVersioningRuleDescriptor getDefaultVersioningRule() {
        return defaultVersioningRuleList.peekLast();
    }

    protected Map<String, VersioningPolicyDescriptor> getVersioningPolicies() {
        return versioningPoliciesRegistry.getVersioningPolicyDescriptors();
    }

    protected Map<String, VersioningFilterDescriptor> getVersioningFilters() {
        return versioningFiltersRegistry.getVersioningFilterDescriptors();
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return service.getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options) {
        service.doPostCreate(doc, options);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel) {
        return service.getSaveOptions(docModel);
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options) {
        return service.isPreSaveDoingCheckOut(doc, isDirty, option, options);
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        return service.doPreSave(doc, isDirty, option, checkinComment, options);
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options) {
        return service.isPostSaveDoingCheckIn(doc, option, options);
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        return service.doPostSave(doc, option, checkinComment, options);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) {
        return service.doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) {
        service.doCheckOut(doc);
    }

    @Override
    public VersioningOption getOptionForAutoVersioning(DocumentModel previousDocument,
            DocumentModel currentDocument) {
        return service.getOptionForAutoVersioning(previousDocument, currentDocument);
    }

}
