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
 *     matic
 */
package org.nuxeo.ecm.automation.server.test;

import java.util.Date;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

/**
 * @author matic
 */
@Operation(id = ReturnOperation.ID, category = Constants.CAT_EXECUTION, label = "True")
public class ReturnOperation {

    public static final String ID = "TestReturn";

    @OperationMethod
    public Date dateValue(Date v) {
        return v;
    }

    @OperationMethod
    public Boolean booleanValue(Boolean v) {
        return v;
    }

    @OperationMethod
    public String stringValue(String v) {
        return v;
    }

    @OperationMethod
    public Integer integerValue(Integer i) {
        return i;
    }

    @OperationMethod
    public Long longValue(Long l) {
        return l;
    }

    @OperationMethod
    public Number integerValue(Number n) {
        return n;
    }

    @OperationMethod
    public Short shortValue(Short s) {
        return s;
    }

    @OperationMethod
    public Double doubleValue(Double d) {
        return d;
    }

    @OperationMethod
    public Float floatValue(Float f) {
        return f;
    }

    @OperationMethod
    public Byte byteValue(Byte b) {
        return b;
    }

    @OperationMethod
    public String[] strings(String[] a) {
        return a;
    }

    @OperationMethod
    public MyObject bean(MyObject b) {
        return b;
    }
}
