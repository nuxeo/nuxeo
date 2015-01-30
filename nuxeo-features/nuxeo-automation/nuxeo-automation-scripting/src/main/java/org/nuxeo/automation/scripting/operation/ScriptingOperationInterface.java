package org.nuxeo.automation.scripting.operation;

import java.util.Map;

public interface ScriptingOperationInterface {

    Object run(Map<String, Object> ctx, Object input, Map<String, Object> parameters);

}
