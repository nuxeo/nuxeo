/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Should be implemented by objects that needs to be returned
 * in response to clients as JSOn objects
 * <p>
 * Implementors are encouraged to use jackson JSON library since it
 * is the one used by automation.
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
 * The value is either a scalar value (from primitive types) either a JSON object <code>{ ... }</code>
 * The type name is the full class name of the serialized object.
 * The primitive types are mapped to a short name as following:
 * <ul>
 *   <li> string
 *   <li> date
 *   <li> boolean
 *   <li> long
 *   <li> double
 *   <li> null - this is a special type in case the objec is null - but
 *   this may never happens (since null objects are returning an empty content)
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface JsonAdapter {

    void toJSON(OutputStream out) throws IOException;

}
