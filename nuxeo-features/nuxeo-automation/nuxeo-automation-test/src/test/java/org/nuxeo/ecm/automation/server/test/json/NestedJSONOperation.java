/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     ogrisel
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * A simple operation that takes raw nested datastructures as input and parameters.
 */
@Operation(id = NestedJSONOperation.ID, category = Constants.CAT_EXECUTION, label = "NestedJSONOperation")
public class NestedJSONOperation {

    public static final String ID = "Operation.NestedJSONOperation";

    public NestedJSONOperation() {
        super();
    }

    @Param(name = "doubleParam", required = false)
    Double doubleParam;

    @Param(name = "pojo", required = false)
    POJOObject pojoParam = new POJOObject();

    @Param(name = "map", required = false)
    Map<String, Object> mapParam = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @OperationMethod
    public POJOObject run(List<String> newItems) {
        String mergedTexts = "Merged texts: ";
        List<String> mergedItems = new ArrayList<String>();
        if (newItems != null) {
            mergedItems.addAll(newItems);
        }
        mergedTexts += pojoParam.getTextContent();
        mergedItems.addAll(pojoParam.getItems());

        if (mapParam.containsKey("textContent")) {
            mergedTexts += mapParam.get("textContent").toString();
        }
        if (mapParam.containsKey("items") && mapParam.get("items") instanceof Collection) {
            mergedItems.addAll((Collection<? extends String>) mapParam.get("items"));
        }
        if (doubleParam != null) {
            mergedItems.add(doubleParam.toString());
        }
        return new POJOObject(mergedTexts, mergedItems);
    }

    @OperationMethod
    public POJOObject run() {
        return run(new ArrayList<String>());
    }

    @OperationMethod
    public POJOObject run(Map<String, Object> input) {
        // perform the mapping from a Map datastructure expected to match the
        // inner structure of POJOObject.
        ObjectMapper mapper = new ObjectMapper();
        return run(mapper.convertValue(input, POJOObject.class).getItems());
    }

    @OperationMethod
    public POJOObject run(POJOObject input) {
        return run(input.getItems());
    }

    @OperationMethod
    public Integer run(Double input) {
        return (int) input.doubleValue();
    }
}
