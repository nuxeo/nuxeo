package org.nuxeo.ecm.webapp.treesorter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.tree2.TreeNode;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * 
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 * 
 */
public class TreeSorterServiceImpl implements TreeSorter {

    private static final Log log = LogFactory.getLog(TreeSorterServiceImpl.class);

    public int getCompareToResult(Object oldObj, Object newObj)
            throws ClientException {

        TreeNode oldNode = (TreeNode) oldObj;
        TreeNode newNode = (TreeNode) newObj;
        return oldNode.getDescription().toUpperCase().compareTo(
                newNode.getDescription().toUpperCase());
    }

}
