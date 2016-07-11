/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationTreeNotAvailable;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeConfigDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublishedDocumentFactoryDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.RootSectionFinderFactoryDescriptor;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinderFactory;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRuleDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * POJO implementation of the publisher service Implements both {@link PublisherService} and
 * {@link RemotePublicationTreeManager}.
 *
 * @author tiry
 */
public class PublisherServiceImpl extends DefaultComponent implements PublisherService, RemotePublicationTreeManager {

    private final Log log = LogFactory.getLog(PublisherServiceImpl.class);

    protected Map<String, PublicationTreeDescriptor> treeDescriptors = new HashMap<String, PublicationTreeDescriptor>();

    protected Map<String, PublishedDocumentFactoryDescriptor> factoryDescriptors = new HashMap<String, PublishedDocumentFactoryDescriptor>();

    protected Map<String, PublicationTreeConfigDescriptor> treeConfigDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();

    protected Map<String, ValidatorsRuleDescriptor> validatorsRuleDescriptors = new HashMap<String, ValidatorsRuleDescriptor>();

    protected Map<String, PublicationTreeConfigDescriptor> pendingDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();

    protected Map<String, PublicationTree> liveTrees = new HashMap<String, PublicationTree>();

    protected RootSectionFinderFactory rootSectionFinderFactory = null;

    // Store association between treeSid and CoreSession that was opened locally
    // for them : this unable proper cleanup of allocated sessions
    protected Map<String, String> remoteLiveTrees = new HashMap<String, String>();

    public static final String TREE_EP = "tree";

    public static final String TREE_CONFIG_EP = "treeInstance";

    public static final String VALIDATORS_RULE_EP = "validatorsRule";

    public static final String FACTORY_EP = "factory";

    public static final String ROOT_SECTION_FINDER_FACTORY_EP = "rootSectionFinderFactory";

    protected static final String ROOT_PATH_KEY = "RootPath";

    protected static final String RELATIVE_ROOT_PATH_KEY = "RelativeRootPath";

    @Override
    public void applicationStarted(ComponentContext context) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // RepositoryService failed to start, no need to go further
            return;
        }
        boolean txWasStartedOutsideComponent = TransactionHelper.isTransactionActiveOrMarkedRollback();

        if (txWasStartedOutsideComponent || TransactionHelper.startTransaction()) {
            boolean completedAbruptly = true;
            try {
                doApplicationStarted();
                completedAbruptly = false;
            } finally {
                if (completedAbruptly) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                if (!txWasStartedOutsideComponent) {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        } else {
            doApplicationStarted();
        }
    }

    protected void doApplicationStarted() {
        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = PublisherServiceImpl.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(nuxeoCL);
            log.info("Publisher Service initialization");
            registerPendingDescriptors();
        } finally {
            Thread.currentThread().setContextClassLoader(jbossCL);
            log.debug("JBoss ClassLoader restored");
        }
    }

    @Override
    public void activate(ComponentContext context) {
        liveTrees = new HashMap<String, PublicationTree>();
        treeDescriptors = new HashMap<String, PublicationTreeDescriptor>();
        factoryDescriptors = new HashMap<String, PublishedDocumentFactoryDescriptor>();
        treeConfigDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();
        validatorsRuleDescriptors = new HashMap<String, ValidatorsRuleDescriptor>();
        pendingDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();
    }

    // for testing cleanup
    public int getLiveTreeCount() {
        return liveTrees.size();
    }

    public PublicationTree getTreeBySid(String sid) {
        return liveTrees.get(sid);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        log.debug("Registry contribution for EP " + extensionPoint);

        if (TREE_EP.equals(extensionPoint)) {
            PublicationTreeDescriptor desc = (PublicationTreeDescriptor) contribution;
            treeDescriptors.put(desc.getName(), desc);
        } else if (TREE_CONFIG_EP.equals(extensionPoint)) {
            PublicationTreeConfigDescriptor desc = (PublicationTreeConfigDescriptor) contribution;
            registerTreeConfig(desc);
        } else if (FACTORY_EP.equals(extensionPoint)) {
            PublishedDocumentFactoryDescriptor desc = (PublishedDocumentFactoryDescriptor) contribution;
            factoryDescriptors.put(desc.getName(), desc);
        } else if (VALIDATORS_RULE_EP.equals(extensionPoint)) {
            ValidatorsRuleDescriptor desc = (ValidatorsRuleDescriptor) contribution;
            validatorsRuleDescriptors.put(desc.getName(), desc);
        } else if (ROOT_SECTION_FINDER_FACTORY_EP.equals(extensionPoint)) {
            RootSectionFinderFactoryDescriptor desc = (RootSectionFinderFactoryDescriptor) contribution;
            try {
                rootSectionFinderFactory = desc.getFactory().newInstance();
            } catch (ReflectiveOperationException t) {
                log.error("Unable to load custom RootSectionFinderFactory", t);
            }
        }
    }

    protected void registerTreeConfig(PublicationTreeConfigDescriptor desc) {
        if (desc.getParameters().get("RelativeRootPath") != null) {
            pendingDescriptors.put(desc.getName(), desc);
        } else {
            treeConfigDescriptors.put(desc.getName(), desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof PublicationTreeDescriptor) {
            treeDescriptors.remove(((PublicationTreeDescriptor) contribution).getName());
        } else if (contribution instanceof PublicationTreeConfigDescriptor) {
            String name = ((PublicationTreeConfigDescriptor) contribution).getName();
            pendingDescriptors.remove(name);
            treeConfigDescriptors.remove(name);
        } else if (contribution instanceof ValidatorsRuleDescriptor) {
            validatorsRuleDescriptors.remove(((ValidatorsRuleDescriptor) contribution).getName());
        } else if (contribution instanceof RootSectionFinderFactoryDescriptor) {
            rootSectionFinderFactory = null;
        }
    }

    protected String computeTreeSessionId(String treeConfigName, CoreSession coreSession) {
        return computeTreeSessionId(treeConfigName, coreSession.getSessionId());
    }

    protected String computeTreeSessionId(String treeConfigName, String sid) {
        return treeConfigName + sid;
    }

    @Override
    public List<String> getAvailablePublicationTree() {
        List<String> treeConfigs = new ArrayList<String>();
        treeConfigs.addAll(treeConfigDescriptors.keySet());
        return treeConfigs;
    }

    @Override
    public Map<String, String> getAvailablePublicationTrees() {
        Map<String, String> trees = new HashMap<String, String>();
        for (PublicationTreeConfigDescriptor desc : treeConfigDescriptors.values()) {
            String title = desc.getTitle() == null ? desc.getName() : desc.getTitle();
            trees.put(desc.getName(), title);
        }
        return trees;
    }

    @Override
    public PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params)
            throws PublicationTreeNotAvailable {
        return getPublicationTree(treeName, coreSession, params, null);
    }

    @Override
    public PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params,
            DocumentModel currentDocument) throws PublicationTreeNotAvailable {
        PublicationTree tree = getOrBuildTree(treeName, coreSession, params);
        if (tree == null) {
            return null;
        }
        if (currentDocument != null) {
            tree.setCurrentDocument(currentDocument);
        }
        return new ProxyTree(tree, tree.getSessionId());
    }

    @Override
    public Map<String, String> initRemoteSession(String treeConfigName, Map<String, String> params) {
        CoreSession coreSession = CoreInstance.openCoreSession(null);
        PublicationTree tree = getPublicationTree(treeConfigName, coreSession, params);

        remoteLiveTrees.put(tree.getSessionId(), coreSession.getSessionId());

        Map<String, String> res = new HashMap<String, String>();
        res.put("sessionId", tree.getSessionId());
        res.put("title", tree.getTitle());
        res.put("nodeType", tree.getNodeType());
        res.put("treeName", tree.getConfigName());
        res.put("path", tree.getPath());

        return res;
    }

    @Override
    public void release(String sid) {
        PublicationTree tree;

        if (liveTrees.containsKey(sid)) {
            tree = liveTrees.get(sid);
            tree.release();
            liveTrees.remove(sid);
        }
        if (remoteLiveTrees.containsKey(sid)) {
            // close here session opened for remote trees
            String sessionId = remoteLiveTrees.get(sid);
            CoreSession remoteSession = Framework.getService(CoreSessionService.class).getCoreSession(sessionId);
            remoteSession.close();
            remoteLiveTrees.remove(sid);
        }
    }

    @Override
    public void releaseAllTrees(String sessionId) {
        for (String configName : treeConfigDescriptors.keySet()) {
            String treeid = computeTreeSessionId(configName, sessionId);
            release(treeid);
        }
    }

    protected PublicationTree getOrBuildTree(String treeConfigName, CoreSession coreSession, Map<String, String> params)
            throws PublicationTreeNotAvailable {
        String key = computeTreeSessionId(treeConfigName, coreSession);
        PublicationTree tree;
        if (liveTrees.containsKey(key)) {
            tree = liveTrees.get(key);
        } else {
            tree = buildTree(key, treeConfigName, coreSession, params);
            if (tree != null) {
                liveTrees.put(key, tree);
            }
        }
        return tree;
    }

    protected PublicationTree buildTree(String sid, String treeConfigName, CoreSession coreSession,
            Map<String, String> params) throws PublicationTreeNotAvailable {
        PublicationTreeConfigDescriptor config = getPublicationTreeConfigDescriptor(treeConfigName);
        Map<String, String> allParameters = computeAllParameters(config, params);
        PublicationTreeDescriptor treeDescriptor = getPublicationTreeDescriptor(config);
        PublishedDocumentFactory publishedDocumentFactory = getPublishedDocumentFactory(config, treeDescriptor,
                coreSession, allParameters);
        return getPublicationTree(treeDescriptor, sid, coreSession, allParameters, publishedDocumentFactory,
                config.getName(), config.getTitle());
    }

    protected Map<String, String> computeAllParameters(PublicationTreeConfigDescriptor config,
            Map<String, String> params) {
        final Map<String, String> allParameters = config.getParameters();
        if (params != null) {
            allParameters.putAll(params);
        }
        return allParameters;
    }

    protected PublishedDocumentFactory getPublishedDocumentFactory(PublicationTreeConfigDescriptor config,
            PublicationTreeDescriptor treeDescriptor, CoreSession coreSession, Map<String, String> params) {
        PublishedDocumentFactoryDescriptor factoryDesc = getPublishedDocumentFactoryDescriptor(config, treeDescriptor);
        ValidatorsRule validatorsRule = getValidatorsRule(factoryDesc);

        PublishedDocumentFactory factory;
        try {
            factory = factoryDesc.getKlass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Error while creating factory " + factoryDesc.getName(), e);
        }
        factory.init(coreSession, validatorsRule, params);
        return factory;
    }

    protected ValidatorsRule getValidatorsRule(PublishedDocumentFactoryDescriptor factoryDesc) {
        String validatorsRuleName = factoryDesc.getValidatorsRuleName();
        ValidatorsRule validatorsRule = null;
        if (validatorsRuleName != null) {
            ValidatorsRuleDescriptor validatorsRuleDesc = validatorsRuleDescriptors.get(validatorsRuleName);
            if (validatorsRuleDesc == null) {
                throw new NuxeoException("Unable to find validatorsRule" + validatorsRuleName);
            }
            try {
                validatorsRule = validatorsRuleDesc.getKlass().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Error while creating validatorsRule " + validatorsRuleName, e);
            }
        }
        return validatorsRule;
    }

    protected PublishedDocumentFactoryDescriptor getPublishedDocumentFactoryDescriptor(
            PublicationTreeConfigDescriptor config, PublicationTreeDescriptor treeDescriptor) {
        String factoryName = config.getFactory();
        if (factoryName == null) {
            factoryName = treeDescriptor.getFactory();
        }

        PublishedDocumentFactoryDescriptor factoryDesc = factoryDescriptors.get(factoryName);
        if (factoryDesc == null) {
            throw new NuxeoException("Unable to find factory" + factoryName);
        }
        return factoryDesc;
    }

    protected PublicationTreeConfigDescriptor getPublicationTreeConfigDescriptor(String treeConfigName) {
        if (!treeConfigDescriptors.containsKey(treeConfigName)) {
            throw new NuxeoException("Unknow treeConfig :" + treeConfigName);
        }
        return treeConfigDescriptors.get(treeConfigName);
    }

    protected PublicationTreeDescriptor getPublicationTreeDescriptor(PublicationTreeConfigDescriptor config) {
        String treeImplName = config.getTree();
        if (!treeDescriptors.containsKey(treeImplName)) {
            throw new NuxeoException("Unknow treeImplementation :" + treeImplName);
        }
        return treeDescriptors.get(treeImplName);
    }

    protected PublicationTree getPublicationTree(PublicationTreeDescriptor treeDescriptor, String sid,
            CoreSession coreSession, Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String treeTitle) throws PublicationTreeNotAvailable {
        PublicationTree treeImpl;
        try {
            treeImpl = treeDescriptor.getKlass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Error while creating tree implementation", e);
        }
        treeImpl.initTree(sid, coreSession, parameters, factory, configName, treeTitle);
        return treeImpl;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        return publish(doc, targetNode, null);
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params) {

        PublicationTree tree = liveTrees.get(targetNode.getSessionId());
        if (tree != null) {
            return tree.publish(doc, targetNode, params);
        } else {
            throw new NuxeoException("Calling getChildrenNodes on a closed tree");
        }
    }

    @Override
    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        PublicationTree tree = liveTrees.get(targetNode.getSessionId());
        if (tree != null) {
            tree.unpublish(doc, targetNode);
        } else {
            throw new NuxeoException("Calling getChildrenNodes on a closed tree");
        }
    }

    @Override
    public void unpublish(String sid, PublishedDocument publishedDocument) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            tree.unpublish(publishedDocument);
        } else {
            throw new NuxeoException("Calling getChildrenNodes on a closed tree");
        }
    }

    @Override
    public List<PublishedDocument> getChildrenDocuments(PublicationNode node) {

        PublicationTree tree = liveTrees.get(node.getSessionId());
        if (tree != null) {
            return tree.getPublishedDocumentInNode(tree.getNodeByPath(node.getPath()));
        } else {
            throw new NuxeoException("Calling getChildrenDocuments on a closed tree");
        }
    }

    protected List<PublicationNode> makeRemotable(List<PublicationNode> nodes, String sid) {
        List<PublicationNode> remoteNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            remoteNodes.add(new ProxyNode(node, sid));
        }

        return remoteNodes;
    }

    @Override
    public List<PublicationNode> getChildrenNodes(PublicationNode node) {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return makeRemotable(tree.getNodeByPath(node.getPath()).getChildrenNodes(), sid);
        } else {
            throw new NuxeoException("Calling getChildrenNodes on a closed tree");
        }
    }

    @Override
    public PublicationNode getParent(PublicationNode node) {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            PublicationNode liveNode;
            liveNode = tree.getNodeByPath(node.getPath()).getParent();
            if (liveNode == null) {
                return null;
            }
            return new ProxyNode(liveNode, sid);
        } else {
            log.error("Calling getParent on a closed tree");
            return null;
        }
    }

    @Override
    public PublicationNode getNodeByPath(String sid, String path) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return new ProxyNode(tree.getNodeByPath(path), sid);
        } else {
            throw new NuxeoException("Calling getNodeByPath on a closed tree");
        }
    }

    @Override
    public List<PublishedDocument> getExistingPublishedDocument(String sid, DocumentLocation docLoc) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.getExistingPublishedDocument(docLoc);
        } else {
            throw new NuxeoException("Calling getNodeByPath on a closed tree");
        }
    }

    @Override
    public List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node) {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.getPublishedDocumentInNode(tree.getNodeByPath(node.getPath()));
        } else {
            throw new NuxeoException("Calling getPublishedDocumentInNode on a closed tree");
        }
    }

    @Override
    public void setCurrentDocument(String sid, DocumentModel currentDocument) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            tree.setCurrentDocument(currentDocument);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public void validatorPublishDocument(String sid, PublishedDocument publishedDocument, String comment) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            tree.validatorPublishDocument(publishedDocument, comment);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public void validatorRejectPublication(String sid, PublishedDocument publishedDocument, String comment) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            tree.validatorRejectPublication(publishedDocument, comment);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public boolean canPublishTo(String sid, PublicationNode publicationNode) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.canPublishTo(publicationNode);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public boolean canUnpublish(String sid, PublishedDocument publishedDocument) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.canUnpublish(publishedDocument);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public boolean canManagePublishing(String sid, PublishedDocument publishedDocument) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.canManagePublishing(publishedDocument);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public boolean isPublishedDocument(DocumentModel documentModel) {
        return PublicationRelationHelper.isPublished(documentModel);
    }

    @Override
    public PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession) {
        PublicationTree tree = null;
        try {
            tree = PublicationRelationHelper.getPublicationTreeUsedForPublishing(doc, coreSession);
        } catch (NuxeoException e) {
            // TODO catch proper exception
            log.error("Unable to get PublicationTree for " + doc.getPathAsString()
                    + ". Fallback on first PublicationTree accepting this document.", e);
            for (String treeName : treeConfigDescriptors.keySet()) {
                tree = getPublicationTree(treeName, coreSession, null);
                if (tree.isPublicationNode(doc)) {
                    break;
                }
            }
        }
        return tree;
    }

    @Override
    public boolean hasValidationTask(String sid, PublishedDocument publishedDocument) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.hasValidationTask(publishedDocument);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public PublishedDocument wrapToPublishedDocument(String sid, DocumentModel documentModel) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.wrapToPublishedDocument(documentModel);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public boolean isPublicationNode(String sid, DocumentModel documentModel) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.isPublicationNode(documentModel);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public PublicationNode wrapToPublicationNode(String sid, DocumentModel documentModel) {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.wrapToPublicationNode(documentModel);
        } else {
            throw new NuxeoException("Calling validatorPublishDocument on a closed tree");
        }
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel, CoreSession coreSession)
            throws PublicationTreeNotAvailable {
        for (String name : getAvailablePublicationTree()) {
            PublicationTree tree = getPublicationTree(name, coreSession, null);
            PublicationTreeConfigDescriptor config = treeConfigDescriptors.get(tree.getConfigName());
            if (!config.islocalSectionTree()) {
                // ignore all non local section tree
                continue;
            }
            if (tree.isPublicationNode(documentModel)) {
                return tree.wrapToPublicationNode(documentModel);
            }
        }
        return null;
    }

    protected void registerPendingDescriptors() {
        // TODO what to do with multiple repositories?
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        String repositoryName = repositoryManager.getDefaultRepositoryName();
        List<DocumentModel> domains = new DomainsFinder(repositoryName).getDomains();
        for (DocumentModel domain : domains) {
            registerTreeConfigFor(domain);
        }
    }

    public void registerTreeConfigFor(DocumentModel domain) {
        for (PublicationTreeConfigDescriptor desc : pendingDescriptors.values()) {
            PublicationTreeConfigDescriptor newDesc = new PublicationTreeConfigDescriptor(desc);
            String newTreeName = desc.getName() + "-" + domain.getName();
            newDesc.setName(newTreeName);
            Path newPath = domain.getPath();
            Map<String, String> parameters = newDesc.getParameters();
            newPath = newPath.append(parameters.remove(RELATIVE_ROOT_PATH_KEY));
            parameters.put(ROOT_PATH_KEY, newPath.toString());
            parameters.put(PublisherService.DOMAIN_NAME_KEY, domain.getTitle());
            treeConfigDescriptors.put(newDesc.getName(), newDesc);
        }
    }

    public void unRegisterTreeConfigFor(DocumentModel domain) {
        unRegisterTreeConfigFor(domain.getName());
    }

    /**
     * @since 7.3
     */
    public void unRegisterTreeConfigFor(String domainName) {
        for (PublicationTreeConfigDescriptor desc : pendingDescriptors.values()) {
            String treeName = desc.getName() + "-" + domainName;
            treeConfigDescriptors.remove(treeName);
            for (Iterator<String> it = liveTrees.keySet().iterator(); it.hasNext();) {
                String entry = it.next();
                if (entry.startsWith(treeName)) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public Map<String, String> getParametersFor(String treeConfigName) {
        PublicationTreeConfigDescriptor desc = treeConfigDescriptors.get(treeConfigName);
        Map<String, String> parameters = new HashMap<String, String>();
        if (desc != null) {
            parameters.putAll(desc.getParameters());
        }
        return parameters;
    }

    @Override
    public RootSectionFinder getRootSectionFinder(CoreSession session) {
        if (rootSectionFinderFactory != null) {
            return rootSectionFinderFactory.getRootSectionFinder(session);
        }
        return new DefaultRootSectionsFinder(session);
    }
}
