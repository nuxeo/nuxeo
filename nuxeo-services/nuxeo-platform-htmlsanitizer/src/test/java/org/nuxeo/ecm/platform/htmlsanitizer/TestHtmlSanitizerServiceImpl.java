/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
public class TestHtmlSanitizerServiceImpl {

    public static final String BAD_HTML = "<b>foo<script>bar</script></b>";

    public static final String SANITIZED_HTML = "<b>foo</b>";

    @Inject
    CoreSession session;

    @Test
    public void sanitizeNote() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setProperty("note", "note", BAD_HTML);
        doc = session.createDocument(doc);
        String note = (String) doc.getProperty("note", "note");
        assertEquals(SANITIZED_HTML, note);
        session.save();
        doc.setProperty("note", "note", BAD_HTML);
        doc = session.saveDocument(doc);
        note = (String) doc.getProperty("note", "note");
        assertEquals(SANITIZED_HTML, note);
    }

}
