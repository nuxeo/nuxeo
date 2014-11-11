package org.nuxeo.elasticsearch.web.sync;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.operations.RefreshUI;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;

@Operation(id = RefreshUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Refresh", description = "Refresh the UI cache. This is a void operation - the input object is returned back as the oputput")
public class RefreshUISync extends RefreshUI {

    @OperationMethod
    public void run() {
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        super.run();
    }

}
