/*
 *    Copyright 2004 Original mockejb authors.
 *    Copyright 2007 Nuxeo SAS.
 *
 * This file is derived from mockejb-0.6-beta2
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
 */
package org.nuxeo.common.mock.jndi;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * MockContext name parser.
 *
 * @author Dimitar Gospodinov
 */
@SuppressWarnings({"ALL"})
class MockContextNameParser implements NameParser {

    private static final Properties syntax = new Properties();
    static {
        syntax.put("jndi.syntax.direction", "left_to_right");
        syntax.put("jndi.syntax.separator", "/");
        syntax.put("jndi.syntax.ignorecase", "false");
        syntax.put("jndi.syntax.trimblanks", "yes");
    }

    /**
     * Parses <code>name</code> into <code>CompoundName</code>
     * using the following <code>CompoundName</code> properties:
     * <pre>
     * jndi.syntax.direction = "left_to_right"
     * jndi.syntax.separator = "/"
     * jndi.syntax.ignorecase = "false"
     * jndi.syntax.trimblanks = "yes"
     * </pre>
     * Any characters '.' in the name <code>name</code> will be replaced with the
     * separator character specified above, before parsing.
     *
     * @param name name to parse
     * @throws NamingException if a naming error occurs
     */
    public Name parse(String name) throws NamingException {
        return new CompoundName(name.replace('.', '/'), syntax);
    }
}
