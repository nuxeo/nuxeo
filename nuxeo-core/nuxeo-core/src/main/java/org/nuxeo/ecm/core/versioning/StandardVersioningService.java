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

import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TO;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION;
import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.ecm.core.api.VersioningOption.MINOR;
import static org.nuxeo.ecm.core.api.VersioningOption.NONE;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOC_LIFE_CYCLE;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.SESSION_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the versioning service that follows standard checkout / checkin semantics.
 */
public class StandardVersioningService implements ExtendableVersioningService {

    private static final Logger log = LogManager.getLogger(StandardVersioningService.class);

    protected static final int DEFAULT_FORMER_RULE_ORDER = 10_000;

    protected static final String COMPAT_ID_PREFIX = "compatibility-type-";

    protected static final String COMPAT_DEFAULT_ID = "compatibility-default";

    /**
     * @deprecated since 9.1 seems unused
     */
    @Deprecated
    public static final String FILE_TYPE = "File";

    /**
     * @deprecated since 9.1 seems unused
     */
    @Deprecated
    public static final String NOTE_TYPE = "Note";

    /**
     * @deprecated since 9.1 seems unused
     */
    @Deprecated
    public static final String PROJECT_STATE = "project";

    public static final String APPROVED_STATE = "approved";

    public static final String OBSOLETE_STATE = "obsolete";

    public static final String BACK_TO_PROJECT_TRANSITION = "backToProject";

    /**
     * @deprecated since 9.1 seems unused
     */
    @Deprecated
    protected static final String AUTO_CHECKED_OUT = "AUTO_CHECKED_OUT";

    /** Key for major version in Document API. */
    protected static final String MAJOR_VERSION = "ecm:majorVersion";

    /** Key for minor version in Document API. */
    protected static final String MINOR_VERSION = "ecm:minorVersion";

    /**
     * @since 9.3
     */
    public static final String CATEGORY = "category";

    /**
     * @since 9.3
     */
    public static final String COMMENT = "comment";

    private Map<String, VersioningPolicyDescriptor> versioningPolicies = new HashMap<>();

    private Map<String, VersioningFilterDescriptor> versioningFilters = new HashMap<>();

    private Map<String, VersioningRestrictionDescriptor> versioningRestrictions = new HashMap<>();

    @Override
    public String getVersionLabel(DocumentModel docModel) {
        String label;
        try {
            label = getMajor(docModel) + "." + getMinor(docModel);
            if (docModel.isCheckedOut() && !"0.0".equals(label)) {
                label += "+";
            }
        } catch (PropertyNotFoundException e) {
            label = "";
        }
        return label;
    }

    protected long getMajor(DocumentModel docModel) {
        return getVersion(docModel, VersioningService.MAJOR_VERSION_PROP);
    }

    protected long getMinor(DocumentModel docModel) {
        return getVersion(docModel, VersioningService.MINOR_VERSION_PROP);
    }

    protected long getVersion(DocumentModel docModel, String prop) {
        Object propVal = docModel.getPropertyValue(prop);
        if (propVal instanceof Long) {
            return ((Long) propVal).longValue();
        } else {
            return 0;
        }
    }

    protected long getMajor(Document doc) {
        return getVersion(doc, MAJOR_VERSION);
    }

    protected long getMinor(Document doc) {
        return getVersion(doc, MINOR_VERSION);
    }

    protected long getVersion(Document doc, String prop) {
        Object propVal = doc.getPropertyValue(prop);
        if (propVal instanceof Long) {
            return ((Long) propVal).longValue();
        } else {
            return 0;
        }
    }

    protected void setVersion(Document doc, long major, long minor) {
        doc.setPropertyValue(MAJOR_VERSION, Long.valueOf(major));
        doc.setPropertyValue(MINOR_VERSION, Long.valueOf(minor));
    }

    protected void incrementMajor(Document doc) {
        setVersion(doc, getMajor(doc) + 1, 0);
    }

    protected void incrementMinor(Document doc) {
        // make sure major is not null by re-setting it
        setVersion(doc, getMajor(doc), getMinor(doc) + 1);
    }

    protected void incrementByOption(Document doc, VersioningOption option) {
        try {
            if (option == MAJOR) {
                incrementMajor(doc);
            } else if (option == MINOR) {
                incrementMinor(doc);
            }
            // else nothing
        } catch (PropertyNotFoundException e) {
            // ignore
        }
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options) {
        if (doc.isVersion() || doc.isProxy()) {
            return;
        }
        setInitialVersion(doc);
    }

    /**
     * Sets the initial version on a document. Can be overridden.
     */
    protected void setInitialVersion(Document doc) {
        // Create a document model for filters
        DocumentModel docModel = DocumentModelFactory.createDocumentModel(doc, null, null);
        InitialStateDescriptor initialState = versioningPolicies.values()
                                                                .stream()
                                                                .sorted()
                                                                .filter(policy -> policy.getInitialState() != null)
                                                                .filter(policy -> isPolicyMatch(policy, null, docModel))
                                                                .map(VersioningPolicyDescriptor::getInitialState)
                                                                .findFirst()
                                                                .orElseGet(InitialStateDescriptor::new);
        setVersion(doc, initialState.getMajor(), initialState.getMinor());
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel) {
        boolean versionable = docModel.isVersionable();
        String lifeCycleState = docModel.getCoreSession().getCurrentLifeCycleState(docModel.getRef());
        String type = docModel.getType();
        return getSaveOptions(versionable, lifeCycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(Document doc) {
        boolean versionable = doc.getType().getFacets().contains(FacetNames.VERSIONABLE);
        String lifeCycleState;
        try {
            lifeCycleState = doc.getLifeCycleState();
        } catch (LifeCycleException e) {
            lifeCycleState = null;
        }
        String type = doc.getType().getName();
        return getSaveOptions(versionable, lifeCycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(boolean versionable, String lifeCycleState, String type) {
        if (!versionable) {
            return Collections.singletonList(NONE);
        }

        // try to get restriction for current type
        List<VersioningOption> options = computeRestrictionOptions(lifeCycleState, type);
        if (options == null) {
            // no specific restrictions on current document type - get restriction for any document type
            options = computeRestrictionOptions(lifeCycleState, "*");
        }
        if (options != null) {
            return options;
        }

        // By default a versionable document could be incremented by all available options
        return Arrays.asList(VersioningOption.values());
    }

    protected List<VersioningOption> computeRestrictionOptions(String lifeCycleState, String type) {
        VersioningRestrictionDescriptor restrictions = versioningRestrictions.get(type);
        if (restrictions != null) {
            // try to get restriction options for current life cycle state
            VersioningRestrictionOptionsDescriptor restrictionOptions = null;
            if (lifeCycleState != null) {
                restrictionOptions = restrictions.getRestrictionOption(lifeCycleState);
            }
            if (restrictionOptions == null) {
                // try to get restriction for any life cycle states
                restrictionOptions = restrictions.getRestrictionOption("*");
            }
            if (restrictionOptions != null) {
                return restrictionOptions.getOptions();
            }
        }
        return null;
    }

    protected VersioningOption validateOption(Document doc, VersioningOption option) {
        List<VersioningOption> options = getSaveOptions(doc);
        // some variables for exceptions
        String type = doc.getType().getName();
        String lifeCycleState;
        try {
            lifeCycleState = doc.getLifeCycleState();
        } catch (LifeCycleException e) {
            lifeCycleState = null;
        }
        if (option == null) {
            if (options.isEmpty() || options.contains(NONE)) {
                // Valid cases:
                // - we don't ask for a version and versioning is blocked by configuration
                // - we don't ask for a version and NONE is available as restriction
                return NONE;
            } else {
                // No version is asked but configuration requires that document must be versioned ie: NONE doesn't
                // appear in restriction contribution
                throw new NuxeoException("Versioning configuration restricts documents with type=" + type
                        + "/lifeCycleState=" + lifeCycleState + " must be versioned for each updates.");
            }
        } else if (!options.contains(option)) {
            throw new NuxeoException("Versioning option=" + option + " is not allowed by the configuration for type="
                    + type + "/lifeCycleState=" + lifeCycleState);
        }
        return option;
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options) {
        boolean disableAutoCheckOut = Boolean.TRUE.equals(options.get(VersioningService.DISABLE_AUTO_CHECKOUT));
        return !doc.isCheckedOut() && isDirty && !disableAutoCheckOut;
    }

    @Override
    public VersioningOption doPreSave(CoreSession session, Document doc, boolean isDirty, VersioningOption option,
            String checkinComment, Map<String, Serializable> options) {
        option = validateOption(doc, option);
        if (isPreSaveDoingCheckOut(doc, isDirty, option, options)) {
            doCheckOut(doc);
            followTransitionByOption(session, doc, options);
        }
        // transition follow shouldn't change what postSave options will be
        return option;
    }

    protected void followTransitionByOption(CoreSession session, Document doc, Map<String, Serializable> options) {
        String lifecycleState = doc.getLifeCycleState();
        if ((APPROVED_STATE.equals(lifecycleState) || OBSOLETE_STATE.equals(lifecycleState))
                && doc.getAllowedStateTransitions().contains(BACK_TO_PROJECT_TRANSITION)) {
            doc.followTransition(BACK_TO_PROJECT_TRANSITION);
            if (session != null) {
                // Send an event to notify that the document state has changed
                sendEvent(session, doc, lifecycleState, options);
            }
        }
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options) {
        // option = validateOption(doc, option); // validated before
        return doc.isCheckedOut() && option != NONE;
    }

    @Override
    public Document doPostSave(CoreSession session, Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        if (isPostSaveDoingCheckIn(doc, option, options)) {
            incrementByOption(doc, option);
            return doc.checkIn(null, checkinComment); // auto-label
        }
        return null;
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) {
        if (option != NONE) {
            incrementByOption(doc, option == MAJOR ? MAJOR : MINOR);
        }
        return doc.checkIn(null, checkinComment); // auto-label
    }

    @Override
    public void doCheckOut(Document doc) {
        Document base = doc.getBaseVersion();
        doc.checkOut();
        // set version number to that of the latest version
        // nothing to do if base is latest version, already at proper version
        if (!base.isLatestVersion()) {
            // this doc was restored from a non-latest version, find the latest one
            Document last = doc.getLastVersion();
            if (last != null) {
                try {
                    setVersion(doc, getMajor(last), getMinor(last));
                } catch (PropertyNotFoundException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    @Deprecated
    public Map<String, VersioningRuleDescriptor> getVersioningRules() {
        return Collections.emptyMap();
    }

    @Override
    @Deprecated
    public void setVersioningRules(Map<String, VersioningRuleDescriptor> versioningRules) {
        // Convert former rules to new one - keep initial state and restriction
        int order = DEFAULT_FORMER_RULE_ORDER - 1;
        for (Entry<String, VersioningRuleDescriptor> rules : versioningRules.entrySet()) {
            String documentType = rules.getKey();
            VersioningRuleDescriptor versioningRule = rules.getValue();
            // Compute policy and filter id
            String compatId = COMPAT_ID_PREFIX + documentType;

            // Convert the rule
            if (versioningRule.isEnabled()) {
                VersioningPolicyDescriptor policy = new VersioningPolicyDescriptor();
                policy.id = compatId;
                policy.order = order;
                policy.initialState = versioningRule.initialState;
                policy.filterIds = new ArrayList<>(Collections.singleton(compatId));

                VersioningFilterDescriptor filter = new VersioningFilterDescriptor();
                filter.id = compatId;
                filter.types = Collections.singleton(documentType);

                // Register rules
                versioningPolicies.put(compatId, policy);
                versioningFilters.put(compatId, filter);

                // Convert save options
                VersioningRestrictionDescriptor restriction = new VersioningRestrictionDescriptor();
                restriction.type = documentType;
                restriction.options = versioningRule.getOptions()
                                                    .values()
                                                    .stream()
                                                    .map(SaveOptionsDescriptor::toRestrictionOptions)
                                                    .collect(Collectors.toMap(
                                                            VersioningRestrictionOptionsDescriptor::getLifeCycleState,
                                                            Function.identity()));
                versioningRestrictions.put(restriction.type, restriction);

                order--;
            } else {
                versioningPolicies.remove(compatId);
                versioningFilters.remove(compatId);
            }
        }
    }

    @Override
    @Deprecated
    public void setDefaultVersioningRule(DefaultVersioningRuleDescriptor defaultVersioningRule) {
        if (defaultVersioningRule == null) {
            return;
        }
        // Convert former rules to new one - keep initial state and restriction
        VersioningPolicyDescriptor policy = new VersioningPolicyDescriptor();
        policy.id = COMPAT_DEFAULT_ID;
        policy.order = DEFAULT_FORMER_RULE_ORDER;
        policy.initialState = defaultVersioningRule.initialState;

        // Register rule
        if (versioningPolicies == null) {
            versioningPolicies = new HashMap<>();
        }
        versioningPolicies.put(policy.id, policy);

        // Convert save options
        VersioningRestrictionDescriptor restriction = new VersioningRestrictionDescriptor();
        restriction.type = "*";
        restriction.options = defaultVersioningRule.getOptions()
                                                   .values()
                                                   .stream()
                                                   .map(SaveOptionsDescriptor::toRestrictionOptions)
                                                   .collect(Collectors.toMap(
                                                           VersioningRestrictionOptionsDescriptor::getLifeCycleState,
                                                           Function.identity()));
        versioningRestrictions.put(restriction.type, restriction);
    }

    @Override
    public void setVersioningPolicies(Map<String, VersioningPolicyDescriptor> versioningPolicies) {
        this.versioningPolicies.clear();
        if (versioningPolicies != null) {
            this.versioningPolicies.putAll(versioningPolicies);
        }
    }

    @Override
    public void setVersioningFilters(Map<String, VersioningFilterDescriptor> versioningFilters) {
        this.versioningFilters.clear();
        if (versioningFilters != null) {
            this.versioningFilters.putAll(versioningFilters);
        }
    }

    @Override
    public void setVersioningRestrictions(Map<String, VersioningRestrictionDescriptor> versioningRestrictions) {
        this.versioningRestrictions.clear();
        if (versioningRestrictions != null) {
            this.versioningRestrictions.putAll(versioningRestrictions);
        }
    }

    @Override
    public void doAutomaticVersioning(DocumentModel previousDocument, DocumentModel currentDocument, boolean before) {
        VersioningPolicyDescriptor policy = retrieveMatchingVersioningPolicy(previousDocument, currentDocument, before);
        if (policy != null && policy.getIncrement() != NONE) {
            if (before) {
                if (previousDocument.isCheckedOut()) {
                    previousDocument.checkIn(policy.getIncrement(), null); // auto label
                    // put back document in checked out state
                    previousDocument.checkOut();
                }
            } else {
                if (currentDocument.isCheckedOut()) {
                    currentDocument.checkIn(policy.getIncrement(), null); // auto label
                }
            }
        }
    }

    protected VersioningPolicyDescriptor retrieveMatchingVersioningPolicy(DocumentModel previousDocument,
            DocumentModel currentDocument, boolean before) {
        return versioningPolicies.values()
                                 .stream()
                                 .filter(policy -> policy.isBeforeUpdate() == before)
                                 // Filter out policy with null increment - possible if we declare a policy for the
                                 // initial state for all documents
                                 .filter(policy -> policy.getIncrement() != null)
                                 .sorted()
                                 .filter(policy -> isPolicyMatch(policy, previousDocument, currentDocument))
                                 .findFirst()
                                 .orElse(null);
    }

    protected boolean isPolicyMatch(VersioningPolicyDescriptor policyDescriptor, DocumentModel previousDocument,
            DocumentModel currentDocument) {
        // Relation between filters in a policy is a AND
        for (String filterId : policyDescriptor.getFilterIds()) {
            VersioningFilterDescriptor filterDescriptor = versioningFilters.get(filterId);
            if (filterDescriptor == null) {
                log.warn("Versioning filter with id={} is referenced in the policy with id={}, but doesn't exist.",
                        filterId, policyDescriptor.getId());
            } else if (!filterDescriptor.newInstance().test(previousDocument, currentDocument)) {
                // As it's a AND, if one fails then policy doesn't match
                return false;
            }
        }
        // All filters match the context (previousDocument + currentDocument)
        log.debug("Document {} is a candidate for {}", currentDocument.getRef(), policyDescriptor);
        return true;
    }

    protected void sendEvent(CoreSession session, Document doc, String previousLifecycleState,
            Map<String, Serializable> options) {
        String sid = session.getSessionId();
        DocumentModel docModel = DocumentModelFactory.createDocumentModel(doc, sid, null);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), docModel);

        ctx.setProperty(TRANSTION_EVENT_OPTION_FROM, previousLifecycleState);
        ctx.setProperty(TRANSTION_EVENT_OPTION_TO, doc.getLifeCycleState());
        ctx.setProperty(TRANSTION_EVENT_OPTION_TRANSITION, BACK_TO_PROJECT_TRANSITION);
        ctx.setProperty(REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(SESSION_ID, sid);
        ctx.setProperty(DOC_LIFE_CYCLE, BACK_TO_PROJECT_TRANSITION);
        ctx.setProperty(CATEGORY, DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY);
        ctx.setProperty(COMMENT, options.get(COMMENT));

        Framework.getService(EventService.class).fireEvent(ctx.newEvent(TRANSITION_EVENT));
    }

}
