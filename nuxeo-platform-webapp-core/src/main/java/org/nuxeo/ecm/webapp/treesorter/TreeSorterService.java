package org.nuxeo.ecm.webapp.treesorter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * 
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 * 
 */
public class TreeSorterService extends DefaultComponent implements TreeSorter {

    private static final Log log = LogFactory.getLog(TreeSorterService.class);

    public static final String TREE_SORTER_EP = "descriptor";

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.webapp.treesorter.TreeSorterService");

    private TreeSorterDescriptor descriptor;

    public TreeSorterService() {
        log.info("Service Media Library Locator created. Not yet proper data.");
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (TREE_SORTER_EP.equals(xp)) {
            Object contribution = extension.getContributions()[0];

            if (contribution instanceof TreeSorterDescriptor) {
                descriptor = (TreeSorterDescriptor) contribution;
                log.info("Have Tree Sorter implementor: "
                        + descriptor.getClassName());
            } else {
                log.warn("Tree Sorter Descriptor not handled: " + contribution);
            }
        } else {
            log.warn("Tree Sorter Descriptor not named: " + xp);
        }
    }

    public int getCompareToResult(Object oldObj, Object newObj)
            throws ClientException {
        if (descriptor == null) {
            log.error("Tree Sorter Sevice not configured, can't work!");
            throw new ClientException("Service not configured!");
        }
        return descriptor.getImplementor().getCompareToResult(oldObj, newObj);
    }

}
