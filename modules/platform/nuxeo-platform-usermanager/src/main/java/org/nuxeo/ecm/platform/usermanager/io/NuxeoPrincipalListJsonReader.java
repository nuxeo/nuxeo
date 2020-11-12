/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalListJsonWriter.ENTITY_TYPE;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.DefaultListJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * see {@link DefaultListJsonReader}
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoPrincipalListJsonReader extends DefaultListJsonReader<NuxeoPrincipal> {

    public NuxeoPrincipalListJsonReader() {
        super(ENTITY_TYPE, NuxeoPrincipal.class);
    }

}
