/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ogrisel
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * A simple operation that takes raw nested datastructures as input and
 * parameters.
 *
 */
@Operation(id = NestedJSONOperation.ID, category = Constants.CAT_EXECUTION, label = "NestedJSONOperation")
public class NestedJSONOperation {

    public static final String ID = "Operation.NestedJSONOperation";

    @Param(name = "doubleParam", required = false)
    Double doubleParam;

    @Param(name = "pojo", required = false)
    POJOObject pojoParam = new POJOObject();

    @Param(name = "map", required = false)
    Map<String, Object> mapParam;

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

        if (mapParam != null && mapParam.containsKey("textContent")) {
            mergedTexts += mapParam.get("textContent").toString();
        }
        if (mapParam != null && mapParam.containsKey("items")
                && mapParam.get("items") instanceof Collection) {
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
    public Integer run(Double input) {
        return (int) input.doubleValue();
    }
}
