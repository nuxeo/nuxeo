package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class SearchVirtualBackend extends AbstractVirtualBackend {

    private static final Log log = LogFactory.getLog(SearchVirtualBackend.class);

    private String query;

    public SearchVirtualBackend(String name, String rootUrl, String query) {
        super(name, rootUrl);
        this.query = query;
    }

    @Override
    protected boolean initIfNeed() throws ClientException {
        if (backendMap == null || orderedBackendNames == null) {
            backendMap = new HashMap<String, Backend>();
            orderedBackendNames = new LinkedList<String>();
            try {
                DocumentModelList docs = getSession().query(query);

                List<String> paths = new ArrayList<String>();
                for (DocumentModel doc : docs) {
                    paths.add(doc.getPathAsString());
                }

                List<String> heads = new ArrayList<String>();
                for (int idx = 0; idx < paths.size(); idx++) {
                    String path = paths.get(idx);
                    if (isHead(path, paths, idx)) {
                        heads.add(path);
                    }
                }

                for (String head : heads) {
                    String headName = new Path(head).lastSegment();
                    String name = headName;
                    int idx = 1;
                    while (backendMap.containsKey(name)) {
                        name = headName + "-" + idx;
                        idx = idx + 1;
                    }

                    Backend simpleBackend = new SimpleBackend(
                            name, head,
                            new Path(this.rootUrl).append(name).toString(),
                            getSession()
                    );
                    registerBackend(simpleBackend);
                }
                return true;
            } catch (ClientException e) {
                log.error("Execute query " + query + " error", e);
            }
        }
        return false;
    }

    private boolean isHead(String path, List<String> paths, int idx) {
        int level = new Path(path).segmentCount();

        for (int i = idx; i >= 0; i--) {
            String other = paths.get(i);
            if (path.contains(other)) {
                if (new Path(other).segmentCount() == level-1) {
                    return false;
                }
            }
        }
        return true;
    }
}
