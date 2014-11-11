/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.xmap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XParent;
import org.w3c.dom.Element;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject
public class Name {

    @XParent
    Author owner;

    @XNode("firstName")
    String firstName;

    @XNode("lastName")
    String lastName;

    @XNode("")
    Element myself;

    @Override
    public String toString() {
        return "Name {\n"
                + "  myself: " + myself + '\n'
                + "  owner: " + owner.getClass() + '#' + owner.hashCode() + '\n'
                + "  firstName: " + firstName + '\n'
                + "  lastName: " + lastName + '\n'
                + '}';
    }

}
