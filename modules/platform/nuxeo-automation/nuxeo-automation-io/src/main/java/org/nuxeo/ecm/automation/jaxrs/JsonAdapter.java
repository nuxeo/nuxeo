/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.jaxrs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Should be implemented by objects that needs to be returned in response to clients as JSOn objects
 * <p>
 * Implementors are encouraged to use jackson JSON library since it is the one used by automation.
 * <p>
 * Also <b>note</b> that the JSON format for an object must follow the following schema:
 *
 * <pre>
 * {
 *   "entity-type": "typeName"
 *   "value": { the marshalled object }
 * }
 * </pre>
 *
 * The value is either a scalar value (from primitive types) either a JSON object <code>{ ... }</code> The type name is
 * the full class name of the serialized object. The primitive types are mapped to a short name as following:
 * <ul>
 * <li>string
 * <li>date
 * <li>boolean
 * <li>long
 * <li>double
 * <li>null - this is a special type in case the objec is null - but this may never happens (since null objects are
 * returning an empty content)
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface JsonAdapter {

    void toJSON(OutputStream out) throws IOException;

}
