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

import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.ecm.core.api.VersioningOption.MINOR;
import static org.nuxeo.ecm.core.api.VersioningOption.NONE;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Implementation of the versioning service that follows standard checkout / checkin semantics.
 */
public class StandardVersioningService implements ExtendableVersioningService {

    private static final Log log = LogFactory.getLog(StandardVersioningService.class);

    public static final String FILE_TYPE = "File";

    public static final String NOTE_TYPE = "Note";

    public static final String PROJECT_STATE = "project";

    public static final String APPROVED_STATE = "approved";

    public static final String OBSOLETE_STATE = "obsolete";

    public static final String BACK_TO_PROJECT_TRANSITION = "backToProject";

    protected static final String AUTO_CHECKED_OUT = "AUTO_CHECKED_OUT";

    /** Key for major version in Document API. */
    protected static final String MAJOR_VERSION = "ecm:majorVersion";

    /** Key for minor version in Document API. */
    protected static final String MINOR_VERSION = "ecm:minorVersion";

    private Map<String, VersioningRuleDescriptor> versioningRules;

    private DefaultVersioningRuleDescriptor defaultVersioningRule;

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
        if (propVal == null || !(propVal instanceof Long)) {
            return 0;
        } else {
            return ((Long) propVal).longValue();
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
        if (propVal == null || !(propVal instanceof Long)) {
            return 0;
        } else {
            return ((Long) propVal).longValue();
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
        InitialStateDescriptor initialState = null;
        if (versioningRules != null) {
            VersioningRuleDescriptor versionRule = versioningRules.get(doc.getType().getName());
            if (versionRule != null) {
                initialState = versionRule.getInitialState();
            }
        }
        if (initialState == null && defaultVersioningRule != null) {
            initialState = defaultVersioningRule.getInitialState();
        }
        if (initialState != null) {
            int initialMajor = initialState.getMajor();
            int initialMinor = initialState.getMinor();
            setVersion(doc, initialMajor, initialMinor);
            return;
        }
        setVersion(doc, 0, 0);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel) {
        boolean versionable = docModel.isVersionable();
        String lifecycleState = docModel.getCoreSession().getCurrentLifeCycleState(docModel.getRef());
        String type = docModel.getType();
        return getSaveOptions(versionable, lifecycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(Document doc) {
        boolean versionable = doc.getType().getFacets().contains(FacetNames.VERSIONABLE);
        String lifecycleState;
        try {
            lifecycleState = doc.getLifeCycleState();
        } catch (LifeCycleException e) {
            lifecycleState = null;
        }
        String type = doc.getType().getName();
        return getSaveOptions(versionable, lifecycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(boolean versionable, String lifecycleState, String type) {
        if (!versionable) {
            return Arrays.asList(NONE);
        }
        if (lifecycleState == null) {
            return Arrays.asList(NONE);
        }
        SaveOptionsDescriptor option = null;
        if (versioningRules != null) {
            VersioningRuleDescriptor saveOption = versioningRules.get(type);
            if (saveOption != null) {
                option = saveOption.getOptions().get(lifecycleState);
                if (option == null) {
                    // try on any life cycle state
                    option = saveOption.getOptions().get("*");
                }
            }
        }
        if (option == null && defaultVersioningRule != null) {
            option = defaultVersioningRule.getOptions().get(lifecycleState);
            if (option == null) {
                // try on any life cycle state
                option = defaultVersioningRule.getOptions().get("*");
            }
        }
        if (option != null) {
            return option.getVersioningOptionList();
        }
        if (PROJECT_STATE.equals(lifecycleState) || APPROVED_STATE.equals(lifecycleState)
                || OBSOLETE_STATE.equals(lifecycleState)) {
            return Arrays.asList(NONE, MINOR, MAJOR);
        }
        if (FILE_TYPE.equals(type) || NOTE_TYPE.equals(type)) {
            return Arrays.asList(NONE, MINOR, MAJOR);
        }
        return Arrays.asList(NONE);
    }

    protected VersioningOption validateOption(Document doc, VersioningOption option) {
        List<VersioningOption> options = getSaveOptions(doc);
        if (!options.contains(option)) {
            option = options.isEmpty() ? NONE : options.get(0);
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
    public VersioningOption doPreSave(Document doc, boolean isDirty, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        option = validateOption(doc, option);
        if (isPreSaveDoingCheckOut(doc, isDirty, option, options)) {
            doCheckOut(doc);
            followTransitionByOption(doc, option);
        }
        // transition follow shouldn't change what postSave options will be
        return option;
    }

    protected void followTransitionByOption(Document doc, VersioningOption option) {
        String lifecycleState = doc.getLifeCycleState();
        if (APPROVED_STATE.equals(lifecycleState) || OBSOLETE_STATE.equals(lifecycleState)) {
            doc.followTransition(BACK_TO_PROJECT_TRANSITION);
        }
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options) {
        // option = validateOption(doc, option); // validated before
        return doc.isCheckedOut() && option != NONE;
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        if (isPostSaveDoingCheckIn(doc, option, options)) {
            incrementByOption(doc, option);
            return doc.checkIn(null, checkinComment); // auto-label
        }
        return null;
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) {
        if (option != VersioningOption.NONE) {
            incrementByOption(doc, option == MAJOR ? MAJOR : MINOR);
        }
        return doc.checkIn(null, checkinComment); // auto-label
    }

    @Override
    public void doCheckOut(Document doc) {
        Document base = doc.getBaseVersion();
        doc.checkOut();
        // set version number to that of the latest version
        if (base.isLatestVersion()) {
            // nothing to do, already at proper version
        } else {
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
    public Map<String, VersioningRuleDescriptor> getVersioningRules() {
        return versioningRules;
    }

    @Override
    public void setVersioningRules(Map<String, VersioningRuleDescriptor> versioningRules) {
        this.versioningRules = versioningRules;
    }

    @Override
    public void setDefaultVersioningRule(DefaultVersioningRuleDescriptor defaultVersioningRule) {
        this.defaultVersioningRule = defaultVersioningRule;
    }

}
