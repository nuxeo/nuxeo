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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Versioning service component and implementation.
 */
public class VersioningComponent extends DefaultComponent implements
        VersioningService {

    private static final Log log = LogFactory.getLog(VersioningComponent.class);

    public static final String XP = "versioningService";

    public static final String VERSIONING_RULE_XP = "versioningRules";

    public VersioningService service;

    protected LinkedList<Class<? extends VersioningService>> contribs;

    protected Map<String, VersioningRuleDescriptor> versioningRules;

    protected LinkedList<DefaultVersioningRuleDescriptor> defaultVersioningRuleList;

    protected boolean recompute;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contribs = new LinkedList<Class<? extends VersioningService>>();
        versioningRules = new HashMap<String, VersioningRuleDescriptor>();
        defaultVersioningRuleList = new LinkedList<DefaultVersioningRuleDescriptor>();
        recompute = true;
        service = null;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        contribs.clear();
        versioningRules.clear();
        defaultVersioningRuleList.clear();
        service = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
        if (XP.equals(xp)) {
            if (!(contrib instanceof VersioningServiceDescriptor)) {
                log.error("Invalid contribution: "
                        + contrib.getClass().getName());
                return;
            }
            VersioningServiceDescriptor desc = (VersioningServiceDescriptor) contrib;
            Class<?> klass;
            try {
                klass = Class.forName(desc.className);
            } catch (ClassNotFoundException e) {
                log.error("Invalid contribution class: " + desc.className);
                return;
            }
            if (!(VersioningService.class.isAssignableFrom(klass))) {
                log.error("Invalid contribution class: " + desc.className);
                return;
            }
            contribs.add((Class<VersioningService>) klass);
            log.info("Registered versioning service: " + desc.className);
            recompute = true;
        } else if (VERSIONING_RULE_XP.equals(xp)) {
            if (contrib instanceof VersioningRuleDescriptor) {
                VersioningRuleDescriptor typeSaveOptDescriptor = (VersioningRuleDescriptor) contrib;
                if (typeSaveOptDescriptor.isEnabled()) {
                    versioningRules.put(typeSaveOptDescriptor.getTypeName(),
                            typeSaveOptDescriptor);
                } else {
                    versioningRules.remove(typeSaveOptDescriptor.getTypeName());
                }
                recompute = true;
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                defaultVersioningRuleList.add((DefaultVersioningRuleDescriptor) contrib);
                recompute = true;
            }
        } else {
            log.error("Unknown extension point " + xp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
        if (XP.equals(xp)) {
            if (!(contrib instanceof VersioningServiceDescriptor)) {
                return;
            }
            VersioningServiceDescriptor desc = (VersioningServiceDescriptor) contrib;
            Class<?> klass;
            try {
                klass = Class.forName(desc.className);
            } catch (ClassNotFoundException e) {
                return;
            }
            if (!(klass.isAssignableFrom(VersioningService.class))) {
                return;
            }
            contribs.remove((Class<VersioningService>) klass);
            log.info("Unregistered versioning service: " + desc.className);
        } else if (VERSIONING_RULE_XP.equals(xp)) {
            if (contrib instanceof VersioningRuleDescriptor) {
                VersioningRuleDescriptor typeSaveOptDescriptor = (VersioningRuleDescriptor) contrib;
                String typeName = typeSaveOptDescriptor.getTypeName();
                if (versioningRules.containsKey(typeName)) {
                    versioningRules.remove(typeName);
                }
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                defaultVersioningRuleList.remove((DefaultVersioningRuleDescriptor) contrib);
            }
            log.info("Unregistered versioning rule: " + contributor.getName());
        }
        recompute = true;
    }

    protected void recompute() {
        Class<? extends VersioningService> klass;
        if (contribs.size() == 0) {
            klass = StandardVersioningService.class;
        } else {
            klass = contribs.getLast();
        }
        if (service == null || klass != service.getClass()) {
            try {
                service = klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } // else keep old service instance

        if (service != null && service instanceof ExtendableVersioningService) {
            ExtendableVersioningService extendableService = (ExtendableVersioningService) service;
            extendableService.setVersioningRules(versioningRules);
            if (!defaultVersioningRuleList.isEmpty()) {
                extendableService.setDefaultVersioningRule(defaultVersioningRuleList.getLast());
            }
        }
    }

    public VersioningService getService() {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service;
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return getService().getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options)
            throws DocumentException {
        getService().doPostCreate(doc, options);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel)
            throws ClientException {
        return getService().getSaveOptions(docModel);
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty,
            VersioningOption option, Map<String, Serializable> options)
            throws DocumentException {
        return getService().isPreSaveDoingCheckOut(doc, isDirty, option,
                options);
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException {
        return getService().doPreSave(doc, isDirty, option, checkinComment, options);
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc,
            VersioningOption option, Map<String, Serializable> options)
            throws DocumentException {
        return getService().isPostSaveDoingCheckIn(doc, option, options);
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option,
            String checkinComment, Map<String, Serializable> options)
            throws DocumentException {
        return getService().doPostSave(doc, option, checkinComment, options);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException {
        return getService().doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) throws DocumentException {
        getService().doCheckOut(doc);
    }
}
