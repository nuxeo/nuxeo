package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jsf.OperationHelper;

@Operation(id = ChangeTab.ID, category = Constants.CAT_UI, requires=Constants.SEAM_CONTEXT,
        label = "Change Tab", description = "Change the selected tab for the current document. Preserve the current input.")
public class ChangeTab {

    public static final String ID = "Seam.ChangeTab";


    protected @Context OperationContext ctx;
    protected @Param(name="tab") String tab;

    @OperationMethod
    public void run() throws Exception {
        OperationHelper.getWebActions().setCurrentTabId(tab);
    }

}
