/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.tests.forms;

import java.net.URL;

import net.sf.json.JSONObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.forms.FormManager;
import org.nuxeo.ecm.webengine.forms.TestFormInstance;
import org.nuxeo.ecm.webengine.forms.validation.Field;
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.MultiStatus;
import org.nuxeo.ecm.webengine.forms.validation.Status;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestForms extends NXRuntimeTestCase {

    protected FormManager formMgr = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-runtime-scripting");
        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core-query");
        deployBundle("nuxeo-core-api");
        deployBundle("nuxeo-core");
        deployBundle("nuxeo-webengine-core");
        deployContrib("OSGI-INF/test-forms.xml");
        WebEngine engine = Framework.getLocalService(WebEngine.class);
        formMgr = engine.getFormManager();
    }

    public void testDeploy() throws Exception {
        Form form = formMgr.getForm("MyForm");
        assertNotNull(form);
        TestFormInstance data = new TestFormInstance();
        data.setField("my:age", "20");
        assertTrue(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "19");
        assertFalse(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "30");
        assertTrue(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "50");
        assertFalse(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "60");
        assertTrue(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "69");
        assertTrue(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", "70");
        assertFalse(form.getField("my:age").validate(data).isOk());
        data.setField("my:age", ""); // age is not required
        assertTrue(form.getField("my:age").validate(data).isOk());

        data.setField("dc:title", ""); // title is required
        assertFalse(form.getField("dc:title").validate(data).isOk());
        data.setField("dc:title", (String)null); // title is required
        assertFalse(form.getField("dc:title").validate(data).isOk());

        data.setField("dc:title", "My little title");
        assertTrue(form.getField("dc:title").validate(data).isOk());
        data.setField("dc:title", "My    little     title");
        assertTrue(form.getField("dc:title").validate(data).isOk());
        data.setField("dc:title", "My very little title");
        assertFalse(form.getField("dc:title").validate(data).isOk());
        data.setField("dc:title", "My title");
        assertFalse(form.getField("dc:title").validate(data).isOk());
        data.setField("dc:title", "My title 2");
        assertFalse(form.getField("dc:title").validate(data).isOk());

        data.setField("my:country", "some country");
        assertFalse(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", "France");
        assertTrue(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", "Romania");
        assertTrue(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", "USA");
        assertTrue(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", "Belgium");
        assertTrue(form.getField("my:country").validate(data).isOk());
        // multi-valued field
        data.addField("my:country", "Romania");
        assertTrue(form.getField("my:country").validate(data).isOk());

        data.setField("my:country", "");
        assertTrue(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", (String)null);
        assertTrue(form.getField("my:country").validate(data).isOk());
        data.setField("my:country", "Italy");
        assertFalse(form.getField("my:country").validate(data).isOk());

        data.setField("my:email", "");
        assertFalse(form.getField("my:email").validate(data).isOk());
        data.setField("my:email", (String)null);
        assertFalse(form.getField("my:email").validate(data).isOk());

        data.setField("confirm_email", "some@email.com");
        assertFalse(form.getField("confirm_email").validate(data).isOk());

        data.setField("my:email", "bs@nuxeo.com");
        assertFalse(form.getField("confirm_email").validate(data).isOk());
        data.setField("confirm_email", "bs@nuxeo.com");
        assertTrue(form.getField("confirm_email").validate(data).isOk());

        // test error message
        data.setField("confirm_email", "some@email.com");
        Status status = form.getField("confirm_email").validate(data);
        assertFalse(status.isOk());
        // message is: "Confirmation E-mail address doesn't match: %s"
        String error = status.getParametrizedMessage(data);
        assertEquals("Confirmation E-mail address doesn't match: some@email.com", error);

        // test JSON representation
        JSONObject obj = new JSONObject().element("isOk", false)
                .element("field", "confirm_email")
                .element("message", "Confirmation E-mail address doesn't match: %s");
        assertEquals(obj, status.toJSON());
    }

    public static void main(String[] args) throws Exception {
        XMap xmap = new XMap();
        URL url = TestForms.class.getResource("test.xml");
        xmap.register(Form.class);
        Object[] objects = xmap.loadAll(url);

        Form form = (Form)objects[0];
        for (Field f : form.getFields().values()) {
            System.out.println(f.getConstraints());
        }

        Field f = form.getField("test-int");
        System.out.println("--------------");
        System.out.println(f.validate(null, "12"));
        System.out.println(f.validate(null, "35"));
        System.out.println(f.validate(null, "40"));
        System.out.println(f.validate(null, "41"));

        f = form.getField("test-string1");
        System.out.println("--------------");
        System.out.println(f.validate(null, "AC@dc"));
        System.out.println(f.validate(null, "AC@Dc"));

        f = form.getField("test-string2");
        System.out.println("--------------");
        System.out.println(f.validate(null, "abc"));
        System.out.println(f.validate(null, "def"));
        System.out.println(f.validate(null, "abcdef"));
        System.out.println(f.validate(null, "abc def"));
        System.out.println(f.validate(null, "ab"));

        f = form.getField("test-string3");
        System.out.println("--------------");
        System.out.println(f.validate(null, "abc"));
        System.out.println(f.validate(null, "def"));
        System.out.println(f.validate(null, "abcdef"));
        System.out.println(f.validate(null, "abc def"));
        System.out.println(f.validate(null, "ab"));

        System.out.println("@@@@@@@@@@");
        TestFormInstance data = new TestFormInstance();
        data.addField("test-int", "12");
        data.addField("test-string1", "AC@dc");
        data.addField("test-string2", "abcdef");
        data.addField("test-string3", "abcdefg");

        Status status = form.validate(data);
        System.out.println(status);
        if (!status.isOk()) {
            if (status.isMultiStatus()) {
                System.out.println(">>> "+status);
                for (Status st : (MultiStatus)status) {
                    System.out.println(">>> "+st);
                }
            } else {
                System.out.println(status);
            }
        }

        System.out.println("@@@@@@@@@@");
        data = new TestFormInstance();
        data.addField("test-int", "12");
        data.addField("test-string1", "AC@dc");
        data.addField("test-string2", "abcdef");
        data.addField("test-string3", "abcdefg");

        status = form.validate(data);
        System.out.println(status);
        if (!status.isOk()) {
            if (status.isMultiStatus()) {
                System.out.println(">>> "+status);
                for (Status st : (MultiStatus)status) {
                    System.out.println(">>> "+st);
                }
            } else {
                System.out.println(status);
            }
        }
    }

}
