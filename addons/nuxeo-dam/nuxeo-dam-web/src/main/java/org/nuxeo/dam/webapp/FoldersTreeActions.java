package org.nuxeo.dam.webapp;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.webapp.helper.DamEventNames.IMPORTSET_CREATED;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNode;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNodeImpl;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.ecm.webapp.tree.TreeManager;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.component.UITree;

/**
 * Action Bean to handle a Folders tree.
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@Name("foldersTree")
@Install(precedence = FRAMEWORK)
@Scope(CONVERSATION)
public class FoldersTreeActions extends TreeActionsBean {

    private static final Log log = LogFactory.getLog(FoldersTreeActions.class);

    // TODO: This method should be remove with the next version to not override
    // all his behavior.
    @Override
    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            DocumentModel currentDocument, String treeName)
            throws ClientException {
        if (treeInvalidator.needsInvalidation()) {
            reset();
            treeInvalidator.invalidationDone();
        }
        List<DocumentTreeNode> currentTree = trees.get(treeName);
        if (currentTree == null) {
            currentTree = new ArrayList<DocumentTreeNode>();
            DamService damService = Framework.getLocalService(DamService.class);
            DocumentModel importRoot = documentManager.getDocument(new PathRef(
                    damService.getAssetLibraryPath()));

            if (importRoot != null) {
                Filter filter = null;
                Filter leafFilter = null;
                Sorter sorter = null;
                String pageProvider = null;
                QueryModel queryModel = null;
                QueryModel orderableQueryModel = null;
                try {
                    TreeManager treeManager = Framework.getService(TreeManager.class);
                    filter = treeManager.getFilter(treeName);
                    leafFilter = treeManager.getLeafFilter(treeName);
                    sorter = treeManager.getSorter(treeName);
                    pageProvider = treeManager.getPageProviderName(treeName);
                    QueryModelDescriptor queryModelDescriptor = treeManager.getQueryModelDescriptor(treeName);
                    queryModel = queryModelDescriptor == null ? null
                            : new QueryModel(queryModelDescriptor);
                    QueryModelDescriptor orderableQueryModelDescriptor = treeManager.getOrderableQueryModelDescriptor(treeName);
                    orderableQueryModel = orderableQueryModelDescriptor == null ? null
                            : new QueryModel(orderableQueryModelDescriptor);
                } catch (Exception e) {
                    log.error("Could not fetch filter or sorter for tree ", e);
                }

                DocumentTreeNode treeRoot = null;
                if (pageProvider == null) {
                    // compatibility code
                    treeRoot = new DocumentTreeNodeImpl(
                            documentManager.getSessionId(), importRoot, filter,
                            leafFilter, sorter, queryModel, orderableQueryModel);
                } else {
                    treeRoot = new DocumentTreeNodeImpl(
                            documentManager.getSessionId(), importRoot, filter,
                            leafFilter, sorter, pageProvider);
                }
                currentTree.add(treeRoot);
                log.debug("Tree initialized with document: "
                        + importRoot.getId());
            } else {
                log.debug("Could not initialize the navigation tree: no parent"
                        + " found for current document");
            }
            trees.put(treeName, currentTree);
        }
        return trees.get(treeName);
    }

    @Override
    public Boolean adviseNodeOpened(UITree treeComponent) {
        if (!isNodeExpandEvent()) {
            Object value = treeComponent.getRowData();
            if (value instanceof DocumentTreeNode) {
                DocumentTreeNode treeNode = (DocumentTreeNode) value;
                DamService damService = Framework.getLocalService(DamService.class);
                if (damService.getAssetLibraryPath().equals(treeNode.getPath())) {
                    return true;
                }
            }
        }
        return null;
    }

    @Observer(value = { IMPORTSET_CREATED })
    public void doReset() {
        reset();
    }
}
