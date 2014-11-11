/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.api.PropertiesDef;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Property;

/**
 * Versions management component implementation.
 *
 * @author Dragos Mihalache
 * @author Florent Guillaume
 */
public class VersioningService extends DefaultComponent implements
        VersioningManager {

    public static final String COMPONENT_ID = "org.nuxeo.ecm.platform.versioning.service.VersioningService";
    public static final String VERSIONING_EXTENSION_POINT_RULES = "rules";
    public static final String VERSIONING_EXTENSION_POINT_PROPERTIES = "properties";

    private static final Log log = LogFactory.getLog(VersioningService.class);

    private final Map<String, EditBasedRuleDescriptor> editRuleDescriptors = new LinkedHashMap<String, EditBasedRuleDescriptor>();
    private final Map<String, AutoBasedRuleDescriptor> autoRuleDescriptors = new LinkedHashMap<String, AutoBasedRuleDescriptor>();
    private final Map<String, VersioningPropertiesDescriptor> propertiesDescriptors = new HashMap<String, VersioningPropertiesDescriptor>();
    private final Map<String, CreateSnapshotDescriptor> snapshotDescriptors = new HashMap<String, CreateSnapshotDescriptor>();

    private String minorVersionProperty;
    private String majorVersionProperty;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);

        minorVersionProperty = getPropertyFallback(context,
                "defaultMinorVersion", PropertiesDef.DOC_PROP_MINOR_VERSION);
        majorVersionProperty = getPropertyFallback(context,
                "defaultMajorVersion", PropertiesDef.DOC_PROP_MAJOR_VERSION);
    }

    private static String getPropertyFallback(ComponentContext context,
            String propName, String defaultValue) {
        final Property p = context.getProperty(propName);
        if (p != null) {
            try {
                return (String) p.getValue();
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        editRuleDescriptors.clear();
        autoRuleDescriptors.clear();
        propertiesDescriptors.clear();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (VERSIONING_EXTENSION_POINT_RULES.equals(extensionPoint)) {
            if (contribution instanceof EditBasedRuleDescriptor) {
                EditBasedRuleDescriptor descriptor = (EditBasedRuleDescriptor) contribution;
                String name = descriptor.getName();
                if (editRuleDescriptors.containsKey(name)) {
                    log.debug("deleting contribution " + name);
                    editRuleDescriptors.remove(name);
                }
                editRuleDescriptors.put(name, descriptor);
                log.debug("added a "
                        + EditBasedRuleDescriptor.class.getSimpleName());
            } else if (contribution instanceof AutoBasedRuleDescriptor) {
                AutoBasedRuleDescriptor descriptor = (AutoBasedRuleDescriptor) contribution;
                String name = descriptor.getName();
                if (autoRuleDescriptors.containsKey(name)) {
                    log.debug("deleting contribution " + name);
                    autoRuleDescriptors.remove(name);
                }
                autoRuleDescriptors.put(name, descriptor);
                log.debug("added a "
                        + AutoBasedRuleDescriptor.class.getSimpleName());
            } else if (contribution instanceof CreateSnapshotDescriptor) {
                CreateSnapshotDescriptor descriptor = (CreateSnapshotDescriptor) contribution;
                String name = descriptor.getName();
                if (snapshotDescriptors.containsKey(name)) {
                    log.debug("override snapshot descriptor: " + name);
                    snapshotDescriptors.remove(name);
                }
                snapshotDescriptors.put(name, descriptor);
                log.debug("added " + descriptor);
            } else {
                log.warn("Descriptor not handled: " + contribution);
            }
        } else if (VERSIONING_EXTENSION_POINT_PROPERTIES.equals(extensionPoint)) {
            VersioningPropertiesDescriptor descriptor = (VersioningPropertiesDescriptor) contribution;
            List<String> docTypes = descriptor.getDocumentTypes();
            if (docTypes != null) {
                for (String docType : docTypes) {
                    if (propertiesDescriptors.containsKey(docType)) {
                        log.debug("Override versioning properties for document type "
                                + docType);
                        propertiesDescriptors.remove(docType);
                    }
                    propertiesDescriptors.put(docType, descriptor);
                }
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        // TODO
    }

    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel document)
            throws ClientException {
        // check Versionable facet
        DocumentType type = document.getDocumentType();
        if (!type.getFacets().contains(FacetNames.VERSIONABLE)) {
            VersionIncEditOptions vincOpt = new VersionIncEditOptions();
            vincOpt.setVersioningAction(VersioningActions.NO_VERSIONING);
            vincOpt.addInfo("Type doesn't have the Versionable facet: "
                    + type.getName());
            return vincOpt;
        }

        // we cannot rely on cached document lifecycle state, so refetch it
        // directly from the core
        if (null == document.getSessionId()) {
            throw new IllegalArgumentException(
                    "document model is not bound to a core session (null sessionId)");
        }
        CoreSession coreSession = CoreInstance.getInstance().getSession(
                document.getSessionId());
        if (coreSession == null) {
            throw new ClientException("cannot get core session for doc: " + document);
        }
        String lifecycleState = coreSession.getCurrentLifeCycleState(document.getRef());

        if (lifecycleState == null) {
            VersionIncEditOptions vincOpt = new VersionIncEditOptions();
            // action is undefined
            vincOpt.addInfo("Document doesn't have a lifecycle state");
            return vincOpt;
        }

        return getVersionIncOptions(lifecycleState, document);
    }

    // FIXME : there is no order on rules, which makes it hard to define which
    // rule will be used first ; use LinkedHashMap for now to use the rule
    // registration order
    public VersionIncEditOptions getVersionIncOptions(String lifecycleState,
            DocumentModel docModel) throws ClientException {

        if (null == lifecycleState) {
            throw new IllegalArgumentException("null lifecycleState ");
        }

        VersionIncEditOptions editOptions = new VersionIncEditOptions();

        for (EditBasedRuleDescriptor descriptor : editRuleDescriptors.values()) {
            if (!descriptor.isEnabled()) {
                continue;
            }
            if (!descriptor.isDocTypeAccounted(docModel.getType())) {
                continue;
            }

            String descriptorLifecycleState = descriptor.getLifecycleState();

            if (!descriptorLifecycleState.equals("*")
                    && !descriptorLifecycleState.equals(lifecycleState)) {
                continue;
            }

            // identified the rule
            editOptions.addInfo("Matching rule descriptor: " + descriptor);

            String action = descriptor.getAction();
            VersioningActions descriptorAction = VersioningActions.getByActionName(action);

            editOptions.addInfo("edit descriptor action: " + action + " => "
                    + descriptorAction);

            // will add options (these are to be displayed to user) only if
            // action is ask_user
            if (VersioningActions.ACTION_CASE_DEPENDENT == descriptorAction) {
                RuleOptionDescriptor[] descOptions = descriptor.getOptions();
                for (RuleOptionDescriptor opt : descOptions) {
                    VersioningActions vAction = VersioningActions.getByActionName(opt.getValue());
                    if (vAction != null) {
                        if (opt.isDefault()) {
                            editOptions.setDefaultVersioningAction(vAction);
                        }
                        editOptions.addOption(vAction);
                    } else {
                        log.warn("Invalid action name: " + opt);
                    }
                }
            }
            editOptions.setVersioningAction(descriptorAction);
            editOptions.addInfo("descriptorAction = " + descriptorAction);

            // apply only the first matching rule
            break;
        }

        log.debug("computed options: " + editOptions);

        return editOptions;
    }

    public void incrementVersions(VersionChangeRequest req)
            throws ClientException {
        if (req.getSource() == VersionChangeRequest.RequestSource.EDIT) {
            for (EditBasedRuleDescriptor descriptor : editRuleDescriptors.values()) {
                if (!descriptor.isEnabled()) {
                    continue;
                }

                // check document type
                String docType = req.getDocument().getType();
                if (!descriptor.isDocTypeAccounted(docType)) {
                    continue;
                }

                // FIXME: match with a specific rule
                boolean handled = performRuleAction(descriptor, req);
                if (handled) {
                    break;
                }
            }

        } else if (req.getSource() == VersionChangeRequest.RequestSource.AUTO) {
            String lifecycleState;
            try {
                lifecycleState = req.getDocument().getCurrentLifeCycleState();
            } catch (ClientException e) {
                log.error(e);
                log.warn("Cannot get CurrentLifeCycleState for document "
                        + req.getDocument());
                // XXX what rule should apply?
                lifecycleState = "";
            }

            boolean handled = false;
            for (AutoBasedRuleDescriptor descriptor : autoRuleDescriptors.values()) {
                if (!descriptor.isEnabled()) {
                    continue;
                }

                final String descriptorLifecycleState = descriptor.getLifecycleState();

                if (descriptorLifecycleState == null) {
                    log.warn("descriptorLifecycleState is null, rule skipped");
                    continue;
                }

                if (descriptorLifecycleState.equals("*")
                        || descriptorLifecycleState.equals(lifecycleState)) {
                    // FIXME: match with a specific rule
                    handled = performRuleAction(descriptor, req);
                    if (handled) {
                        break;
                    }
                }
            }
            // no versioning rule that can be applied
            if (!handled) {
                final VersioningActions action = req.getVersioningAction();
                if (action == null) {
                    log.warn("versioning action is null, inc version aborted.");
                    return;
                }
                performRuleAction(action, req.getDocument());
            }
        } else {
            log.warn("Not handled: " + req);
        }
    }

    private boolean performRuleAction(RuleDescriptor descriptor,
            VersionChangeRequest req) throws ClientException {

        String descActionName = descriptor.getAction();

        final VersioningActions descAction = VersioningActions.getByActionName(descActionName);
        if (descAction == null) {
            log.error(String.format(
                    "invalid action name: %s in descriptor: %s",
                    descActionName, descriptor.getName()));
            return false;
        }

        VersioningActions action = descAction;
        if (action.equals(VersioningActions.ACTION_CASE_DEPENDENT)) {
            action = req.getVersioningAction();
        }

        // if editaction.. identify option and perform additional operations if
        // specified
        // like: perform lifecycle transition
        // AT: yeah, me too.
        if (descriptor instanceof EditBasedRuleDescriptor) {
            RuleOptionDescriptor[] opts = ((EditBasedRuleDescriptor) descriptor).getOptions();

            DocumentModel doc = req.getDocument();
            String currentDocLifeCycleState;
            try {
                CoreSession coreSession = CoreInstance.getInstance().getSession(
                        doc.getSessionId());
                if (null == coreSession) {
                    throw new ClientException(
                            "cannot get core session for doc: " + doc);
                }
                currentDocLifeCycleState = coreSession.getCurrentLifeCycleState(doc.getRef());
            } catch (ClientException e) {
                log.warn("cannot get lifecycle to perform possible transition "
                        + "specified by versioning in rule options "
                        + descriptor.getName(), e);
                return false;
            }

            // check lifecycle
            String descriptorLifecycleState = ((EditBasedRuleDescriptor) descriptor).getLifecycleState();
            if (!descriptorLifecycleState.equals("*")
                    && !descriptorLifecycleState.equals(currentDocLifeCycleState)) {
                return false;
            }

            for (RuleOptionDescriptor option : opts) {
                String optionValue = option.getValue();
                if (optionValue == null) {
                    log.error("RuleOptionDescriptor name not defined in RuleDescriptor: "
                            + descriptor);
                    continue;
                }

                VersioningActions selectedOption = req.getVersioningAction();

                // NXP-1236 : apply default incrementation option
                if (selectedOption == VersioningActions.ACTION_INCREMENT_DEFAULT
                        && option.isDefault()) {
                    action = VersioningActions.getByActionName(optionValue);
                    selectedOption = action; // to perform lifecycle
                    // transition
                }

                if (selectedOption == VersioningActions.getByActionName(optionValue)) {
                    // apply eventually specified lifecycle transition
                    String lsTrans = option.getLifecycleTransition();
                    if (lsTrans != null) {
                        try {
                            doc.followTransition(lsTrans);
                        } catch (ClientException e) {
                            throw new ClientException(
                                    "cannot perform lifecycle transition: "
                                            + lsTrans
                                            + " specified by versioning rule:option "
                                            + descriptor.getName() + ":"
                                            + optionValue, e);
                        }
                    }
                }
            }
        }

        return performRuleAction(action, req.getDocument());
    }

    private boolean performRuleAction(VersioningActions action,
            DocumentModel doc) throws ClientException {
        if (action.equals(VersioningActions.ACTION_INCREMENT_MAJOR)) {
            incrementMajor(doc);
        } else if (action.equals(VersioningActions.ACTION_INCREMENT_MINOR)) {
            incrementMinor(doc);
        } else {
            log.debug("<incrementVersions> action not recognized: " + action);
            return false;
        }

        return true;
    }

    private static long getValidVersionNumber(DocumentModel doc, String propName)
            throws ClientException {
        Object propVal = doc.getProperty(
                DocumentModelUtils.getSchemaName(propName),
                DocumentModelUtils.getFieldName(propName));
        long ver = 0;
        if (propVal != null) {
            try {
                ver = ((Long) propVal).longValue();
            } catch (ClassCastException e) {
                throw new ClientException("Property " + propName
                        + " should be of type Long");
            }
        }
        return ver;
    }

    public DocumentModel incrementMajor(DocumentModel document)
            throws ClientException {
        String docType = document.getType();
        String majorPropName = getMajorVersionPropertyName(docType);
        String minorPropName = getMinorVersionPropertyName(docType);

        long major = getValidVersionNumber(document, majorPropName) + 1;
        long minor = 0;
        document.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                DocumentModelUtils.getFieldName(majorPropName),
                Long.valueOf(major));
        document.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                DocumentModelUtils.getFieldName(minorPropName),
                Long.valueOf(minor));
        return document;
    }

    public DocumentModel incrementMinor(DocumentModel document)
            throws ClientException {
        String docType = document.getType();
        String majorPropName = getMajorVersionPropertyName(docType);
        String minorPropName = getMinorVersionPropertyName(docType);

        long major = getValidVersionNumber(document, majorPropName);
        long minor = getValidVersionNumber(document, minorPropName) + 1;
        document.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                DocumentModelUtils.getFieldName(majorPropName),
                Long.valueOf(major));
        document.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                DocumentModelUtils.getFieldName(minorPropName),
                Long.valueOf(minor));
        return document;
    }

    public String getVersionLabel(DocumentModel document) throws ClientException {
        String documentType = document.getType();
        String majorPropName = getMajorVersionPropertyName(documentType);
        String minorPropName = getMinorVersionPropertyName(documentType);

        long major = getValidVersionNumber(document, majorPropName);
        long minor = getValidVersionNumber(document, minorPropName);
        return major + "." + minor;
    }

    /**
     * Returns the default property name to use when setting the major version.
     */
    public String getDefaultMajorVersionPropertyName() {
        return majorVersionProperty;
    }

    /**
     * Returns the default property name to use when setting the minor version.
     */
    public String getDefaultMinorVersionPropertyName() {
        return minorVersionProperty;
    }

    /**
     * Returns the property name to use when setting the major version for this
     * document type or default property name if not explicitly set for the
     * given document type.
     */
    public String getMajorVersionPropertyName(String documentType) {
        if (propertiesDescriptors.containsKey(documentType)) {
            VersioningPropertiesDescriptor descriptor = propertiesDescriptors.get(documentType);
            String majorVersionField = descriptor.getMajorVersion();
            if (majorVersionField != null) {
                return majorVersionField;
            }
        }
        return majorVersionProperty;
    }

    /**
     * Returns the property name to use when setting the minor version for this
     * document type or default property name if not explicitly set for the
     * given document type.
     */
    public String getMinorVersionPropertyName(String documentType) {
        if (propertiesDescriptors.containsKey(documentType)) {
            VersioningPropertiesDescriptor descriptor = propertiesDescriptors.get(documentType);
            String minorVersionField = descriptor.getMinorVersion();
            if (minorVersionField != null) {
                return minorVersionField;
            }
        }
        return minorVersionProperty;
    }

    public SnapshotOptions getCreateSnapshotOption(DocumentModel document)
            throws ClientException {
        // we cannot rely on cached document lifecycle state, so refetch it
        // directly from the core
        if (null == document.getSessionId()) {
            throw new IllegalArgumentException(
                    "document model is not bound to a core session (null sessionId)");
        }
        CoreSession coreSession = CoreInstance.getInstance().getSession(
                document.getSessionId());
        if (coreSession == null) {
            throw new ClientException("cannot get core session for doc: " + document);
        }
        String lifecycleState = coreSession.getCurrentLifeCycleState(document.getRef());
        if (lifecycleState == null) {
            log.error("Cannot get lifecycle state for doc " + document);
            return SnapshotOptions.UNDEFINED;
        }

        for (CreateSnapshotDescriptor desc : snapshotDescriptors.values()) {
            if (desc.applyForLifecycleState(lifecycleState)) {
                return desc.getSnapshotOption();
            }
        }
        return SnapshotOptions.UNDEFINED;
    }

}
