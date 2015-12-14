package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id = SleepOperation.ID, category = Constants.CAT_EXECUTION, label = "Sleep", description =
        "Sleep for durationMillis.")
public class SleepOperation {

    public static final String ID = "SleepOperation";

    @Param(name = "durationMillis", required = false)
    protected long durationMillis = 0;

    @OperationMethod
    public void run() {
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
