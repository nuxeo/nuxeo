package org.nuxeo.ecm.automation.server.test;

import org.codehaus.jackson.node.ArrayNode;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id="ArraysBoundOp")
public class ArraysBoundOperation {

    public static String ID = "ArraysBoundOp";

    @Param(name="json")
    ArrayNode values;

    @OperationMethod
    public void run() {
        System.out.println(values);
    }
}
