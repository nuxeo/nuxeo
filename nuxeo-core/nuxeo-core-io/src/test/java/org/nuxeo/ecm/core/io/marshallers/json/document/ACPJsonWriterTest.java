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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class ACPJsonWriterTest extends AbstractJsonWriterTest.Local<ACPJsonWriter, ACP> {

    public ACPJsonWriterTest() {
        super(ACPJsonWriter.class, ACP.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myDoc", "Document");
        document = session.createDocument(document);
        UserEntry entry = new UserEntryImpl("SpongeBob");
        entry.addPrivilege("DancingWithPatrickStar");
        document.getACP().setRules("SpongeBobRules", new UserEntry[] { entry });
    }

    @Test
    public void test() throws Exception {
        JsonAssert json = jsonAssert(document.getACP());
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("acls");
        json = json.has("acl").isArray();
        json.length(2);
        json.has(0).has("name").isEquals("inherited");
        json = json.has(1);
        json.has("name").isEquals("SpongeBobRules");
        json = json.has("ace").length(1);
        json = json.has(0).isObject();
        json.has("username").isEquals("SpongeBob");
        json.has("permission").isEquals("DancingWithPatrickStar");
        json.has("granted").isTrue();
    }

}
