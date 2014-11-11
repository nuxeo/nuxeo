package org.nuxeo.ecm.webapp.treesorter;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * 
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 * 
 */
public interface TreeSorter {

    int getCompareToResult(Object oldObj, Object newObj) throws ClientException;

}
