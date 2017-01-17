/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.schema.types;

import org.nuxeo.ecm.core.schema.SchemaNames;

/**
 * The removed type.
 *
 * @since 9.1
 */
public class RemovedType extends AbstractType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "removed";

    public static final RemovedType INSTANCE = new RemovedType();

    private RemovedType() {
        super(null, SchemaNames.BUILTIN, ID);
    }

    @Override
    public Object convert(Object value) throws TypeException {
        // TODO maybe log something here ?
        return null;
    }

}
