/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Thimonier
 */
package org.nuxeo.ecm.core.uidgen;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/test-uidgenerator-contrib.xml")
public class TestDocUIDGeneratorListener {

    @Inject
    protected CoreSession session;

    @Inject
    protected UIDGeneratorService service;

    @Test
    public void testListener() {
        DocumentModel doc = session.createDocumentModel("/", "testFile", "Note");
        doc.setPropertyValue("dc:format", "FOO");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        String uid = (String) doc.getPropertyValue("uid:uid");
        int year = new GregorianCalendar().get(Calendar.YEAR);
        assertEquals("FOO" + year + "00001", uid);
    }

}
