/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Make it possible to directly JSON tree nodes parsed by the REST API (e.g. parameters or input) directly to java
 * datastructures.
 *
 * @author Olivier Grisel
 * @since 5.7
 */
public class ObjectNodeToMap implements TypeAdapter {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        return mapper.convertValue(objectToAdapt, Map.class);
    }

}
