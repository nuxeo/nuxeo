/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.api.DocVersion;
import org.nuxeo.ecm.platform.versioning.api.Evaluator;
import org.nuxeo.ecm.platform.versioning.api.PropertiesDef;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningException;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.platform.versioning.wfintf.WFState;
import org.nuxeo.ecm.platform.versioning.wfintf.WFVersioningPolicyProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Property;

/**
 * Versions management component implementation.
 *
 * @author : <a href="dm@nuxeo.com">Dragos Mihalache</a>
 */
public class VersioningService extends DefaultComponent implements
        VersioningManager {

    public static final String COMPONENT_ID = "org.nuxeo.ecm.platform.versioning.service.VersioningService";

    public static final String VERSIONING_EXTENSION_POINT_RULES = "rules";

    public static final String VERSIONING_EXTENSION_POINT_PROPERTIES = "properties";

    private static final Log log = LogFactory.getLog(VersioningService.class);

    private String minorVersionProperty;

    private String majorVersionProperty;

    private final Map<String, WFBasedRuleDescriptor> wfRuleDescriptors = new LinkedHashMap<String, WFBasedRuleDescriptor>();

    private final Map<String, EditBasedRuleDescriptor> editRuleDescriptors = new LinkedHashMap<String, EditBasedRuleDescriptor>();

    private final Map<String, AutoBasedRuleDescriptor> autoRuleDescriptors = new LinkedHashMap<String, AutoBasedRuleDescriptor>();

    private final Map<String, VersioningPropertiesDescriptor> propertiesDescriptors = new HashMap<String, VersioningPropertiesDescriptor>();

    private final Map<String, CreateSnapshotDescriptor> snapshotDescriptors = new HashMap<String, CreateSnapshotDescriptor>();

    public VersioningService() {
        log.debug("<init>");
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        log.debug("<activate>");
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
        log.debug("<deactivate>");
        super.deactivate(context);

        wfRuleDescriptors.clear();
        editRuleDescriptors.clear();
        autoRuleDescriptors.clear();
        propertiesDescriptors.clear();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (VERSIONING_EXTENSION_POINT_RULES.equals(extensionPoint)) {
            if (contribution instanceof WFBasedRuleDescriptor) {
                WFBasedRuleDescriptor descriptor = (WFBasedRuleDescriptor) contribution;
                String name = descriptor.getName();
                if (wfRuleDescriptors.containsKey(name)) {
                    log.debug("deleting contribution " + name);
                    wfRuleDescriptors.remove(name);
                }
                wfRuleDescriptors.put(name, descriptor);
                log.debug("added a "
                        + WFBasedRuleDescriptor.class.getSimpleName());
            } else if (contribution instanceof EditBasedRuleDescriptor) {
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

    // ----- versioning api impl --------
    public VersionIncEditOptions getVersionIncEditOptions(@NotNull DocumentModel docModel)
            throws VersioningException, ClientException, DocumentException {
        if (null == docModel.getSessionId()) {
            throw new IllegalArgumentException(
                    "document model is not bound to a core session (null sessionId)");
        }

        final CoreSession coreSession = CoreInstance.getInstance().getSession(docModel.getSessionId());
        if (null == coreSession) {
            throw new ClientException("cannot get core session for doc: " + docModel);
        }
        String lifecycleState = coreSession.getCurrentLifeCycleState(docModel.getRef());

        //DocumentType type = NXSchema.getSchemaManager().getDocumentType(docType);
        DocumentType type = docModel.getDocumentType();

        // check if we have versionable facet for the given doc type
        if (!type.getFacets().contains(FacetNames.VERSIONABLE)) {
            final VersionIncEditOptions vincOpt = new VersionIncEditOptions();
            vincOpt.setVersioningAction(VersioningActions.NO_VERSIONING);
            vincOpt.addInfo("no versioning for doc type '" + type.getName() + "'");
            return vincOpt;
        }

        //DocumentModel doc = getDocumentModel(docRef);

        final boolean wfInProgress = WFState.hasWFProcessInProgress(docModel);

        // TODO fixme - hardcoded state
        if (wfInProgress) {
            log.debug("workflow in progress");
            lifecycleState = "review";
        }

        if (lifecycleState == null) {
            final VersionIncEditOptions vincOpt = new VersionIncEditOptions();
            vincOpt.addInfo("document doesn't have a lifecycle state, cannot determine v inc options");
            return vincOpt;
        }

        VersionIncEditOptions versIncOpts = getVersionIncOptions(
                lifecycleState, type.getName(), docModel);

        // if the rule says to query workflow do so
        if (versIncOpts.getVersioningAction() == VersioningActions.ACTION_QUERY_WORKFLOW) {
            versIncOpts.addInfo("check versioning policy in document workflow");

            final VersioningActions wfvaction = WFVersioningPolicyProvider.getVersioningPolicyFor(docModel);
            versIncOpts.addInfo("wfvaction = " + wfvaction);

            if (wfvaction != null) {
                versIncOpts.clearOptions();
                // return null;// wfvaction;
                if (wfvaction == VersioningActions.ACTION_CASE_DEPENDENT) {
                    versIncOpts.addOption(VersioningActions.ACTION_NO_INCREMENT);
                    versIncOpts.addOption(VersioningActions.ACTION_INCREMENT_MINOR);
                } else {
                    // because LE needs options we add the option received from
                    // WF
                    // also the rule is that if wf specified an inc option
                    // that one is to be added
                    // set default so it will appear selected
                    wfvaction.setDefault(true);
                    versIncOpts.addOption(wfvaction);
                }

                versIncOpts.setVersioningAction(wfvaction);
            } else {
                // XXX wf action is null!!?
                log.error("wf action is null");
                versIncOpts.addInfo("wf action is null");

                versIncOpts.setVersioningAction(null);
            }
        }

        return versIncOpts;
    }

    private DocumentModel getDocumentModel(DocumentRef docRef) throws VersioningException{
        CoreSession coreSession = null;
        try {
            // make sure we'll have an authenticated thread
            LoginContext loginContext = Framework.login();
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repo= mgr.getDefaultRepository();
            coreSession = repo.open();
            return coreSession.getDocument(docRef);
        } catch (Exception e) {
            //log.error("cannot retrieve document for ref: " + docRef ,e);
            throw new VersioningException("cannot retrieve document for ref: " + docRef ,e);
        }
        finally {
            if (coreSession != null) {
                CoreInstance.getInstance().close(coreSession);
            }
        }
    }

    /**
     * Edit: an increment option is asked to a user who edits a document in
     * various lifecycle states.
     *
     * @param lifecycleState
     * @return
     *
     * @deprecated parameters are insuficient for a full evaluation. Will be
     *             removed. Use getVersionIncEditOptions(...)
     */
    @Deprecated
    public VersionIncEditOptions getVersionIncOptions(String lifecycleState,
            String docType) {

        try {
            return getVersionIncOptions(lifecycleState, docType, null);
        } catch (ClientException e) {
            log.error(
                    "Cannot get version increment options for lifecycleState: "
                            + lifecycleState + " and docType: " + docType, e);
            return null;
        }
    }

    /**
     * Needed to keep API compatibility (will be deprecated)
     *
     * @param lifecycleState
     * @param docType - redundant when docModel is not null
     * @param docModel
     * @return
     * @throws ClientException
     */
    // FIXME : there is no order on rules, which makes it hard to define which
    // rule will be used first ; use LinkedHashMap for now to use the rule
    // registration order
    private VersionIncEditOptions getVersionIncOptions(String lifecycleState,
            String docType, DocumentModel docModel) throws ClientException {

        final String logPrefix = "<getVersionIncOptions> ";

        if (null == lifecycleState) {
            throw new IllegalArgumentException("null lifecycleState ");
        }
        if (null == docType) {
            throw new IllegalArgumentException("null docType ");
        }

        log.debug(logPrefix + "lifecycle state : " + lifecycleState);
        log.debug(logPrefix + "docType         : " + docType);
        log.debug(logPrefix + "edit descriptors: " + editRuleDescriptors);

        final VersionIncEditOptions editOptions = new VersionIncEditOptions();

        for (EditBasedRuleDescriptor descriptor : editRuleDescriptors.values()) {
            if (!descriptor.isEnabled()) {
                continue;
            }
            if (!descriptor.isDocTypeAccounted(docType)) {
                log.debug(logPrefix + "rule descriptor excluded for doc type: "
                        + docType);
                continue;
            }

            final String descriptorLifecycleState = descriptor.getLifecycleState();

            if (!descriptorLifecycleState.equals("*")
                    && !descriptorLifecycleState.equals(lifecycleState)) {
                log.debug(logPrefix
                        + "rule descriptor excluded for lifecycle: "
                        + lifecycleState);
                continue;
            }

            // identified the rule
            log.debug(logPrefix + "rule descriptor to apply: " + descriptor);
            editOptions.addInfo("Matching rule descriptor: " + descriptor);

            final String action = descriptor.getAction();
            final VersioningActions descriptorAction = VersioningActions.getByActionName(action);

            final String info = "edit descriptor action: " + action + " => "
                    + descriptorAction;
            log.debug(logPrefix + info);
            editOptions.addInfo(info);

            /*
             * VersioningActions va = VersioningActions
             * .getByActionName(action); if (va != null) options.add(va);
             */

            // will add options (these are to be displayed to user) only if
            // action is ask_user
            if (VersioningActions.ACTION_CASE_DEPENDENT == descriptorAction) {
                log.debug(logPrefix + "Action case_dependent, adding options ");
                final RuleOptionDescriptor[] descOptions = descriptor.getOptions();
                for (RuleOptionDescriptor opt : descOptions) {

                    // check if there is a specified evaluator to display or not this option
                    Evaluator evaluator = opt.getEvaluator();
                    if (evaluator != null) {
                        if (docModel != null) {
                            if (!evaluator.evaluate(docModel)) {
                                log.info("option not added (evaluated to false) "
                                        + opt);
                                continue;
                            }
                        }
                        else {
                            log.warn("Cannot invoke evaluator with null document for option: "
                                    + opt);
                        }
                    }

                    VersioningActions vAction = VersioningActions.getByActionName(opt.getValue());
                    if (vAction != null) {
                        vAction.setDefault(opt.isDefault());
                        editOptions.addOption(vAction);
                    } else {
                        log.warn("Invalid action name: " + opt);
                    }
                }

                editOptions.setVersioningAction(VersioningActions.ACTION_CASE_DEPENDENT);
                editOptions.addInfo("Action case dependent");

            } else {
                log.debug(logPrefix + "descriptorAction = " + descriptorAction
                        + "; no option for user specified by rule.");

                // TODO maybe set it in one place, see above
                editOptions.setVersioningAction(descriptorAction);
                editOptions.addInfo("descriptorAction = " + descriptorAction);
            }

            // apply only the first matching rule
            break;
        }

        log.debug(logPrefix + "computed options: " + editOptions);

        return editOptions;
    }

    public void incrementVersions(VersionChangeRequest req)
            throws ClientException {
        final String logPrefix = "<incrementVersions> ";

        if (req.getSource() == VersionChangeRequest.RequestSource.WORKFLOW) {
            boolean ruleFound = false;
            for (WFBasedRuleDescriptor descriptor : wfRuleDescriptors.values()) {
                if (!descriptor.isEnabled()) {
                    continue;
                }

                if (descriptor.getWorkflowStateInitial().equals(
                        req.getWfInitialState())
                        && descriptor.getWorkflowStateFinal().equals(
                                req.getWfFinalState())) {

                    log.debug("applying lifecycle rule: "
                            + descriptor.getName());

                    // we detected a match - perform action
                    performRuleAction(descriptor, req);
                    ruleFound = true;
                }
            }

            if (!ruleFound) {
                log.warn(logPrefix + "No matching rule found for request: "
                        + req);
            }

        } else if (req.getSource() == VersionChangeRequest.RequestSource.EDIT) {
            for (EditBasedRuleDescriptor descriptor : editRuleDescriptors.values()) {
                if (!descriptor.isEnabled()) {
                    continue;
                }

                // check document type
                String docType = req.getDocument().getType();
                if (!descriptor.isDocTypeAccounted(docType)) {
                    log.debug(logPrefix + "rule descriptor excluded for doc type: "
                            + docType);
                    continue;
                }
                log.debug(logPrefix + "rule descriptor matching doc type: " + docType);

                // FIXME: match with a specific rule
                boolean handled = performRuleAction(descriptor, req);
                if (handled) {
                    break;
                }
            }

        } else if (req.getSource() == VersionChangeRequest.RequestSource.AUTO) {
            log.debug(logPrefix + "autoRuleDescriptors #: "
                    + autoRuleDescriptors.size());

            String lifecycleState;
            try {
                lifecycleState = req.getDocument().getCurrentLifeCycleState();
            } catch (ClientException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.warn(logPrefix
                        + "Cannot get CurrentLifeCycleState for document "
                        + req.getDocument());
                lifecycleState = ""; // XXX: what rule ???
            }

            log.debug(logPrefix + "CurrentLifeCycleState : " + lifecycleState);
            boolean handled = false;
            for (AutoBasedRuleDescriptor descriptor : autoRuleDescriptors.values()) {
                if (!descriptor.isEnabled()) {
                    continue;
                }

                log.debug(logPrefix + "applying autoRuleDescriptors #: "
                        + descriptor);

                final String descriptorLifecycleState = descriptor.getLifecycleState();

                if (null == descriptorLifecycleState) {
                    log.warn(logPrefix
                            + "descriptorLifecycleState is null, rule skipped");
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
                log.debug("No (AUTO) rule matched. Perform action specified by the caller: "
                        + action);
                if (action == null) {
                    log.warn("versioning action is null, inc version aborted.");
                    return;
                }
                performRuleAction(action, req.getDocument());
            }
        } else {
            log.warn(logPrefix + "<incrementVersions> not handled: " + req);
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
        if (descriptor instanceof EditBasedRuleDescriptor) {
            RuleOptionDescriptor[] opts = ((EditBasedRuleDescriptor) descriptor).getOptions();

            DocumentModel doc = req.getDocument();
            String currentDocLifeCycleState;
            try {
                CoreSession coreSession = CoreInstance.getInstance().getSession(doc.getSessionId());
                if (null == coreSession) {
                    throw new ClientException("cannot get core session for doc: " + doc);
                }
                //currentDocLifeCycleState = doc.getCurrentLifeCycleState();
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
                // log.debug("not applied for lifecycle: " + lifecycleState);
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
                if (selectedOption == VersioningActions.ACTION_INCREMENT_DEFAULT && option.isDefault()) {
                    action = VersioningActions.getByActionName(optionValue);
                    selectedOption = action; // to perform lifecycle transition
                }

                if (selectedOption == VersioningActions.getByActionName(optionValue)) {
                    // apply eventually specified lifecycle transition
                    String lsTrans = option.getLifecycleTransition();
                    if (lsTrans != null) {

                        log.info("followTransition: " + lsTrans);

                        try {
                            doc.followTransition(lsTrans);
                        } catch (ClientException e) {
                            throw new ClientException(
                                    "cannot perform lifecycle transition: "
                                            + lsTrans
                                            + " specified by versioning rule:option "
                                            + descriptor.getName() + ":"
                                            + optionValue);
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
        final Object propVal = doc.getProperty(
                DocumentModelUtils.getSchemaName(propName),
                DocumentModelUtils.getFieldName(propName));

        long ver = 0;
        if (null == propVal) {
            // versions not initialized
        } else {
            try {
                ver = (Long) propVal;
            } catch (ClassCastException e) {
                throw new ClientException("Property " + propName
                        + " should be of type Long");
            }
        }

        return ver;
    }

    public DocumentModel incrementMajor(DocumentModel doc) throws ClientException {
        String documentType = doc.getType();
        String majorPropName = getMajorVersionPropertyName(documentType);
        String minorPropName = getMinorVersionPropertyName(documentType);

        long major = getValidVersionNumber(doc, majorPropName);
        long minor = getValidVersionNumber(doc, minorPropName);

        major++;
        minor = 0;

        doc.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                DocumentModelUtils.getFieldName(majorPropName), major);
        doc.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                DocumentModelUtils.getFieldName(minorPropName), minor);

        log.debug("<incrementMajor> DocumentModel (major=" + major + ", minor="
                + minor + ")");

        return doc;
    }

    public DocumentModel incrementMinor(DocumentModel doc) throws ClientException {
        String documentType = doc.getType();
        String majorPropName = getMajorVersionPropertyName(documentType);
        String minorPropName = getMinorVersionPropertyName(documentType);

        long major = getValidVersionNumber(doc, majorPropName);
        long minor = getValidVersionNumber(doc, minorPropName);

        minor++;

        doc.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                DocumentModelUtils.getFieldName(majorPropName), major);
        doc.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                DocumentModelUtils.getFieldName(minorPropName), minor);

        log.debug("<incrementMinor> DocumentModel (major=" + major + ", minor="
                + minor + ")");

        return doc;
    }

    public String getVersionLabel(DocumentModel doc) throws ClientException {
        String documentType = doc.getType();
        String majorPropName = getMajorVersionPropertyName(documentType);
        String minorPropName = getMinorVersionPropertyName(documentType);

        long major = getValidVersionNumber(doc, majorPropName);
        long minor = getValidVersionNumber(doc, minorPropName);

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
     * document type or default property name if not explicitly set for the given
     * document type.
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
     * document type or default property name if not explicitly set for the given
     * document type.
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

    // TODO: this is obviously not implemented.
    public DocVersion getNextVersion(DocumentModel documentModel)
            throws ClientException {
        throw new UnsupportedOperationException("not implemented");
    }

    public SnapshotOptions getCreateSnapshotOption(DocumentModel document) throws ClientException {
        // we cannot rely on cached document lifecycle state, getting it directly
        // from the core
        // String lifecycleState = document.getCurrentLifeCycleState();
        if (null == document.getSessionId()) {
            throw new IllegalArgumentException(
                    "document model is not bound to a core session (null sessionId)");
        }

        final CoreSession coreSession = CoreInstance.getInstance().getSession(document.getSessionId());
        if (null == coreSession) {
            throw new ClientException("cannot get core session for doc: " + document);
        }
        String lifecycleState = coreSession.getCurrentLifeCycleState(document.getRef());

        if (null == lifecycleState) {
            log.error("Cannot get lifecycle state for doc " + document);
            return SnapshotOptions.UNDEFINED;
        }

        Collection<CreateSnapshotDescriptor> descriptorsIt = snapshotDescriptors.values();
        for (CreateSnapshotDescriptor createSnapshotDescriptor : descriptorsIt) {
            if (createSnapshotDescriptor.applyForLifecycleState(lifecycleState)) {

                // check special conditions if any
                boolean skip = false;
                for (Evaluator evaluator : createSnapshotDescriptor.getEvaluators()) {

                    if (!evaluator.evaluate(document)) {
                        log.debug("snapshot descriptor "
                                + createSnapshotDescriptor.getName()
                                + " skipped because of negative evaluation of "
                                + evaluator.getClass());

                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    break;
                }

                log.debug("found snapshot descriptor: "
                        + createSnapshotDescriptor.getName());
                return createSnapshotDescriptor.getSnapshotOption();
            }
        }
        log.debug("couldn't find a matching snapshot descriptor for doc "
                + document.getTitle() + " with lifecycle state: "
                + lifecycleState);
        return SnapshotOptions.UNDEFINED;
    }

}
