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

import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * A simple operation that takes raw nested datastructures as input and parameters.
 */
@Operation(id = JSONOperationWithArrays.ID, category = Constants.CAT_EXECUTION, label = "JSONOperationWithArrays")
public class JSONOperationWithArrays {

    public static final String ID = "Operation.JSONOperationWithArrays";

    @Param(name = "pojos", required = true)
    protected List<SimplePojo> pojos;

    // @Param(name = "pojo1", required = true)
    // protected SimplePojo pojo1;

    @OperationMethod
    public SimplePojo run() {
        for (SimplePojo pojo : pojos) {
            System.out.println(pojo.getName());
        }
        return pojos.isEmpty() ? null : pojos.get(0);
        // if (pojo1 != null) {
        // System.out.println(pojo1.getName());
        // }
        // return pojo1;
    }

    public static class SimplePojo {

        private String name;

        public SimplePojo() {
        }

        public SimplePojo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}
