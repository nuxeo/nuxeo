/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Implementation of the versioning service that follows standard checkout /
 * checkin semantics.
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
    protected static final String MAJOR_VERSION = "major_version";

    /** Key for minor version in Document API. */
    protected static final String MINOR_VERSION = "minor_version";

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
        } catch (ClientException e) {
            log.debug("No version label", e);
            label = "";
        }
        return label;
    }

    protected long getMajor(DocumentModel docModel) throws ClientException {
        return getVersion(docModel, VersioningService.MAJOR_VERSION_PROP);
    }

    protected long getMinor(DocumentModel docModel) throws ClientException {
        return getVersion(docModel, VersioningService.MINOR_VERSION_PROP);
    }

    protected long getVersion(DocumentModel docModel, String prop)
            throws ClientException {
        Object propVal = docModel.getPropertyValue(prop);
        if (propVal == null || !(propVal instanceof Long)) {
            return 0;
        } else {
            return ((Long) propVal).longValue();
        }
    }

    protected long getMajor(Document doc) throws DocumentException {
        return getVersion(doc, MAJOR_VERSION);
    }

    protected long getMinor(Document doc) throws DocumentException {
        return getVersion(doc, MINOR_VERSION);
    }

    protected long getVersion(Document doc, String prop)
            throws DocumentException {
        Object propVal = doc.getPropertyValue(prop);
        if (propVal == null || !(propVal instanceof Long)) {
            return 0;
        } else {
            return ((Long) propVal).longValue();
        }
    }

    protected void setVersion(Document doc, long major, long minor)
            throws DocumentException {
        doc.setPropertyValue(MAJOR_VERSION, Long.valueOf(major));
        doc.setPropertyValue(MINOR_VERSION, Long.valueOf(minor));
    }

    protected void incrementMajor(Document doc) throws DocumentException {
        setVersion(doc, getMajor(doc) + 1, 0);
    }

    protected void incrementMinor(Document doc) throws DocumentException {
        doc.setPropertyValue("minor_version", Long.valueOf(getMinor(doc) + 1));
    }

    protected void incrementByOption(Document doc, VersioningOption option)
            throws DocumentException {
        try {
            if (option == MAJOR) {
                incrementMajor(doc);
            } else if (option == MINOR) {
                incrementMinor(doc);
            }
            // else nothing
        } catch (NoSuchPropertyException e) {
            // ignore
        }
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options) {
        if (doc.isVersion() || doc.isProxy()) {
            return;
        }
        try {
            setInitialVersion(doc);
        } catch (DocumentException e) {
            // ignore
        }
    }

    /**
     * Sets the initial version on a document. Can be overridden.
     */
    protected void setInitialVersion(Document doc) throws DocumentException {
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
    public List<VersioningOption> getSaveOptions(DocumentModel docModel)
            throws ClientException {
        boolean versionable = docModel.isVersionable();
        String lifecycleState;
        try {
            lifecycleState = docModel.getCoreSession().getCurrentLifeCycleState(
                    docModel.getRef());
        } catch (ClientException e) {
            lifecycleState = null;
        }
        String type = docModel.getType();
        return getSaveOptions(versionable, lifecycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(Document doc)
            throws DocumentException {
        boolean versionable = doc.getType().getFacets().contains(
                FacetNames.VERSIONABLE);
        String lifecycleState;
        try {
            lifecycleState = doc.getLifeCycleState();
        } catch (LifeCycleException e) {
            lifecycleState = null;
        }
        String type = doc.getType().getName();
        return getSaveOptions(versionable, lifecycleState, type);
    }

    protected List<VersioningOption> getSaveOptions(boolean versionable,
            String lifecycleState, String type) {
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
        if (PROJECT_STATE.equals(lifecycleState)
                || APPROVED_STATE.equals(lifecycleState)
                || OBSOLETE_STATE.equals(lifecycleState)) {
            return Arrays.asList(NONE, MINOR, MAJOR);
        }
        if (FILE_TYPE.equals(type) || NOTE_TYPE.equals(type)) {
            return Arrays.asList(NONE, MINOR, MAJOR);
        }
        return Arrays.asList(NONE);
    }

    protected VersioningOption validateOption(Document doc,
            VersioningOption option) throws DocumentException {
        List<VersioningOption> options = getSaveOptions(doc);
        if (!options.contains(option)) {
            option = options.get(0);
        }
        return option;
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException {
        option = validateOption(doc, option);
        if (!doc.isCheckedOut() && isDirty) {
            doCheckOut(doc);
            followTransitionByOption(doc, option);
        }
        // transition follow shouldn't change what postSave options will be
        return option;
    }

    protected void followTransitionByOption(Document doc,
            VersioningOption option) throws DocumentException {
        try {
            String lifecycleState = doc.getLifeCycleState();
            if (APPROVED_STATE.equals(lifecycleState)
                    || OBSOLETE_STATE.equals(lifecycleState)) {
                doc.followTransition(BACK_TO_PROJECT_TRANSITION);
            }
        } catch (LifeCycleException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option,
            String checkinComment, Map<String, Serializable> options)
            throws DocumentException {
        // option = validateOption(doc, option); // validated before
        boolean increment = option != NONE;
        if (doc.isCheckedOut() && increment) {
            incrementByOption(doc, option);
            return doc.checkIn(null, checkinComment); // auto-label
        }
        return null;
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException {
        if (option != VersioningOption.NONE) {
            incrementByOption(doc, option == MAJOR ? MAJOR : MINOR);
        }
        return doc.checkIn(null, checkinComment); // auto-label
    }

    @Override
    public void doCheckOut(Document doc) throws DocumentException {
        doc.checkOut();
        // set version number to that of the last version
        try {
            Document last = doc.getLastVersion();
            if (last != null) {
                setVersion(doc, getMajor(last), getMinor(last));
            }
        } catch (NoSuchPropertyException e) {
            // ignore
        }
    }

    @Override
    public Map<String, VersioningRuleDescriptor> getVersioningRules() {
        return versioningRules;
    }

    @Override
    public void setVersioningRules(
            Map<String, VersioningRuleDescriptor> versioningRules) {
        this.versioningRules = versioningRules;
    }

    @Override
    public void setDefaultVersioningRule(
            DefaultVersioningRuleDescriptor defaultVersioningRule) {
        this.defaultVersioningRule = defaultVersioningRule;
    }

}
