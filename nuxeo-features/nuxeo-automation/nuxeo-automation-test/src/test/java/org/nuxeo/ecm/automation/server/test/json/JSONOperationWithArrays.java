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

    @Param(name = "pojoList", required = false)
    protected List<SimplePojo> pojoList;

    @Param(name = "pojoListList", required = false)
    protected List<List<SimplePojo>> pojosListList;

    @Param(name = "pojoArray", required = false)
    protected SimplePojo[] pojoArray;

    @Param(name = "pojo", required = false)
    protected SimplePojo pojo;

    @Param(name = "whichPojo")
    protected String whichPojo;

    @OperationMethod
    public SimplePojo run() {
        switch (whichPojo) {
        case "pojo":
            return pojo;
        case "pojoList":
            return pojoList.get(0);
        case "pojoListList":
            return pojosListList.get(0).get(0);
        case "pojoArray":
            return pojoArray[0];
        default:
            return null;
        }
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
