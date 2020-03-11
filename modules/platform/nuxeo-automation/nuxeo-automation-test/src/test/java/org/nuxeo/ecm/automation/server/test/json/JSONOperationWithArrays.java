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
