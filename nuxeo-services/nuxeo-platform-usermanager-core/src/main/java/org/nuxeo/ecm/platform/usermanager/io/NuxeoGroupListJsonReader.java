/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupListJsonWriter.ENTITY_TYPE;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.io.marshallers.json.DefaultListJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * see {@link DefaultListJsonReader}
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoGroupListJsonReader extends DefaultListJsonReader<NuxeoGroup> {

    public NuxeoGroupListJsonReader() {
        super(ENTITY_TYPE, NuxeoGroup.class);
    }

}
