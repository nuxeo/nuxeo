package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.publisher.api.*;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeConfigDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeDescriptor;
import org.nuxeo.ecm.platform.publisher.descriptors.PublishedDocumentFactoryDescriptor;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRuleDescriptor;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * 
 * Pojo implementation of the publisher service Implements both
 * {@link PublisherService} and {@link RemotePublicationTreeManager}
 * 
 * @author tiry
 * 
 */
public class PublisherServiceImpl extends DefaultComponent implements
        PublisherService, RemotePublicationTreeManager {

    protected static Map<String, PublicationTreeDescriptor> treeDescriptors = new HashMap<String, PublicationTreeDescriptor>();

    protected static Map<String, PublishedDocumentFactoryDescriptor> factoryDescriptors = new HashMap<String, PublishedDocumentFactoryDescriptor>();

    protected static Map<String, PublicationTreeConfigDescriptor> treeConfigDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();

    protected static Map<String, ValidatorsRuleDescriptor> validatorsRuleDescriptors = new HashMap<String, ValidatorsRuleDescriptor>();

    private static final Log log = LogFactory.getLog(PublisherServiceImpl.class);

    protected static Map<String, PublicationTree> liveTrees = new HashMap<String, PublicationTree>();

    public static final String TREE_EP = "tree";

    public static final String TREE_CONFIG_EP = "treeInstance";

    public static final String VALIDATORS_RULE_EP = "validatorsRule";

    public static final String FACTORY_EP = "factory";

    @Override
    public void activate(ComponentContext context) throws Exception {
        liveTrees = new HashMap<String, PublicationTree>();
        treeDescriptors = new HashMap<String, PublicationTreeDescriptor>();
        factoryDescriptors = new HashMap<String, PublishedDocumentFactoryDescriptor>();
        treeConfigDescriptors = new HashMap<String, PublicationTreeConfigDescriptor>();
        validatorsRuleDescriptors = new HashMap<String, ValidatorsRuleDescriptor>();
    }

    // for testing cleanup
    public static int getLiveTreeCount() {
        return liveTrees.size();
    }

    public static PublicationTree getTreeBySid(String sid) {
        return liveTrees.get(sid);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        log.debug("Registry contribution for EP " + extensionPoint);

        if (TREE_EP.equals(extensionPoint)) {
            PublicationTreeDescriptor desc = (PublicationTreeDescriptor) contribution;
            treeDescriptors.put(desc.getName(), desc);
        } else if (TREE_CONFIG_EP.equals(extensionPoint)) {
            PublicationTreeConfigDescriptor desc = (PublicationTreeConfigDescriptor) contribution;
            treeConfigDescriptors.put(desc.getName(), desc);
        } else if (FACTORY_EP.equals(extensionPoint)) {
            PublishedDocumentFactoryDescriptor desc = (PublishedDocumentFactoryDescriptor) contribution;
            factoryDescriptors.put(desc.getName(), desc);
        } else if (VALIDATORS_RULE_EP.equals(extensionPoint)) {
            ValidatorsRuleDescriptor desc = (ValidatorsRuleDescriptor) contribution;
            validatorsRuleDescriptors.put(desc.getName(), desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    protected String computeTreeSessionId(String treeConfigName,
            CoreSession coreSession) {
        return treeConfigName + coreSession.getSessionId();
    }

    public List<String> getAvailablePublicationTree() {
        List<String> treeConfigs = new ArrayList<String>();
        treeConfigs.addAll(treeConfigDescriptors.keySet());
        return treeConfigs;
    }

    public PublicationTree getPublicationTree(String treeName,
            CoreSession coreSession, Map<String, String> params)
            throws ClientException {
        PublicationTree tree = getOrBuildTree(treeName, coreSession, params);
        return new ProxyTree(tree, tree.getSessionId());
    }

    public Map<String, String> initRemoteSession(String treeConfigName,
            Map<String, String> params) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        CoreSession coreSession = null;
        if (rm != null) {
            coreSession = rm.getDefaultRepository().open();
        }

        PublicationTree tree = getPublicationTree(treeConfigName, coreSession,
                params);

        Map<String, String> res = new HashMap<String, String>();
        res.put("sessionId", tree.getSessionId());
        res.put("title", tree.getTitle());
        res.put("nodeType", tree.getNodeType());
        res.put("treeName", tree.getConfigName());
        res.put("path", tree.getPath());

        return res;
    }

    public void release(String sid) {
        PublicationTree tree;

        if (liveTrees.containsKey(sid)) {
            tree = liveTrees.get(sid);
            tree.release();
            liveTrees.remove(sid);
        }
    }

    protected PublicationTree getOrBuildTree(String treeConfigName,
            CoreSession coreSession, Map<String, String> params) {
        String key = computeTreeSessionId(treeConfigName, coreSession);
        PublicationTree tree;

        if (liveTrees.containsKey(key)) {
            tree = liveTrees.get(key);
        } else {
            tree = buildTree(key, treeConfigName, coreSession, params);
            if (tree != null)
                liveTrees.put(key, tree);
        }
        return tree;
    }

    protected PublicationTree buildTree(String sid, String treeConfigName,
            CoreSession coreSession, Map<String, String> params) {
        if (!treeConfigDescriptors.containsKey(treeConfigName)) {
            log.error("Unknow treeConfig :" + treeConfigName);
            return null;
        }

        PublicationTreeConfigDescriptor config = treeConfigDescriptors.get(treeConfigName);

        String treeImplName = config.getTree();

        if (!treeDescriptors.containsKey(treeImplName)) {
            log.error("Unknow treeImplementation :" + treeImplName);
            return null;
        }

        PublicationTreeDescriptor treeDesc = treeDescriptors.get(treeImplName);

        PublicationTree treeImpl;

        try {
            treeImpl = treeDesc.getKlass().newInstance();
        } catch (Exception e) {
            log.error("Error while creating tree implementation", e);
            return null;
        }

        String factoryName = config.getFactory();
        if (factoryName == null) {
            factoryName = treeDesc.getFactory();
        }

        PublishedDocumentFactoryDescriptor factoryDesc = factoryDescriptors.get(factoryName);
        if (factoryDesc == null) {
            log.error("Unable to find factory" + factoryName);
            return null;
        }

        PublishedDocumentFactory factory;

        try {
            factory = factoryDesc.getKlass().newInstance();
        } catch (Exception e) {
            log.error("Error while creating factory " + factoryName, e);
            return null;
        }

        Map<String, String> p = config.getParameters();
        if (params != null) {
            p.putAll(params);
        }

        try {
            factory.init(coreSession, p);
        } catch (Exception e) {
            log.error("Error during Factory init", e);
            return null;
        }

        String validatorsRuleName = config.getValidatorsRule();
        ValidatorsRule validatorsRule = null;
        if (validatorsRuleName != null) {
            ValidatorsRuleDescriptor validatorsRuleDesc = validatorsRuleDescriptors.get(validatorsRuleName);
            if (validatorsRuleDesc == null) {
                log.error("Unable to find validatorsRule" + validatorsRuleName);
                return null;
            }
            try {
                validatorsRule = validatorsRuleDesc.getKlass().newInstance();
            } catch(Exception e) {
                log.error("Error while creating validatorsRule " + validatorsRuleName, e);
                return null;
            }
        }

        try {
            treeImpl.initTree(sid, coreSession, p, factory, config.getName(), validatorsRule);
        } catch (Exception e) {
            log.error("Error ducing tree init", e);
            return null;
        }

        return treeImpl;
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        return publish(doc, targetNode, null);
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        PublicationTree tree = liveTrees.get(targetNode.getSessionId());
        if (tree != null) {
            return tree.publish(doc, targetNode, params);
        } else {
            throw new ClientException(
                    "Calling getChildrenNodes on a closed tree");
        }
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {
        PublicationTree tree = liveTrees.get(targetNode.getSessionId());
        if (tree != null) {
            tree.unpublish(doc, targetNode);
        } else {
            throw new ClientException(
                    "Calling getChildrenNodes on a closed tree");
        }
    }

    public void unpublish(String sid, PublishedDocument publishedDocument)
            throws ClientException {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            tree.unpublish(publishedDocument);
        } else {
            throw new ClientException(
                    "Calling getChildrenNodes on a closed tree");
        }
    }

    public List<PublishedDocument> getChildrenDocuments(PublicationNode node)
            throws ClientException {

        PublicationTree tree = liveTrees.get(node.getSessionId());
        if (tree != null) {
            return tree.getPublishedDocumentInNode(tree.getNodeByPath(node.getPath()));
        } else {
            throw new ClientException(
                    "Calling getChildrenDocuments on a closed tree");
        }
    }

    protected List<PublicationNode> makeRemotable(List<PublicationNode> nodes,
            String sid) throws ClientException {
        List<PublicationNode> remoteNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            remoteNodes.add(new ProxyNode(node, sid));
        }

        return remoteNodes;
    }

    public List<PublicationNode> getChildrenNodes(PublicationNode node)
            throws ClientException {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return makeRemotable(
                    tree.getNodeByPath(node.getPath()).getChildrenNodes(), sid);
        } else {
            throw new ClientException(
                    "Calling getChildrenNodes on a closed tree");
        }
    }

    public PublicationNode getParent(PublicationNode node) {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            PublicationNode liveNode;
            try {
                liveNode = tree.getNodeByPath(node.getPath()).getParent();
                return new ProxyNode(liveNode, sid);
            } catch (ClientException e) {
                log.error("Error while getting Parent", e);
                return null;
            }
        } else {
            log.error("Calling getParent on a closed tree");
            return null;
        }
    }

    public PublicationNode getNodeByPath(String sid, String path)
            throws ClientException {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return new ProxyNode(tree.getNodeByPath(path), sid);
        } else {
            throw new ClientException("Calling getNodeByPath on a closed tree");
        }
    }

    public List<PublishedDocument> getExistingPublishedDocument(String sid,
            DocumentLocation docLoc) throws ClientException {
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.getExistingPublishedDocument(docLoc);
        } else {
            throw new ClientException("Calling getNodeByPath on a closed tree");
        }
    }

    public List<PublishedDocument> getPublishedDocumentInNode(
            PublicationNode node) throws ClientException {
        String sid = node.getSessionId();
        PublicationTree tree = liveTrees.get(sid);
        if (tree != null) {
            return tree.getPublishedDocumentInNode(tree.getNodeByPath(node.getPath()));
        } else {
            throw new ClientException(
                    "Calling getPublishedDocumentInNode on a closed tree");
        }
    }

}
