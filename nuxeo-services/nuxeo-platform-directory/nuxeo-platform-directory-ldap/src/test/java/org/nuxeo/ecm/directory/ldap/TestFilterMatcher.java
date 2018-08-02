/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.junit.Test;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public class TestFilterMatcher {

    @Test
    public void testEmptyFilterMatching() {
        LDAPFilterMatcher matcher = new LDAPFilterMatcher();
        assertTrue(matcher.match(null, null));
        assertTrue(matcher.match(null, ""));
    }

    @Test
    public void testAtomicFilter() throws Exception {
        AttributesImpl attributes = new AttributesImpl();
        attributes.put("attribute1", "value1");
        attributes.put("attribute2", "val$ue2");
        attributes.put("attribute3", "val^ue3");
        attributes.put("attribute4", "val(ue)4");
        attributes.put("attribute5", "value?5");
        attributes.put("attribute6", "value.+6");

        LDAPFilterMatcher matcher = new LDAPFilterMatcher();

        // check simple values with wild cards
        assertTrue(matcher.match(attributes, ""));
        assertTrue(matcher.match(attributes, "(attribute1=*)"));
        assertTrue(matcher.match(attributes, "(attribute1=value1)"));
        assertTrue(matcher.match(attributes, "(attribute1=Value1)"));
        assertTrue(matcher.match(attributes, "(attribute1=VALUE1)"));
        assertTrue(matcher.match(attributes, "(attribute1=*lue1)"));
        assertTrue(matcher.match(attributes, "(attribute1=val*)"));
        assertTrue(matcher.match(attributes, "(attribute1=*al*)"));
        assertTrue(matcher.match(attributes, "(attribute1=*AL*)"));
        assertTrue(matcher.match(attributes, "(attribute1=v*l*1)"));
        assertTrue(matcher.match(attributes, "(attribute1=*v*l*1*)"));
        assertFalse(matcher.match(attributes, "(attribute1=value2)"));

        // check with special chars in values
        assertTrue(matcher.match(attributes, "(attribute2=val$ue2)"));
        assertTrue(matcher.match(attributes, "(attribute2=val$u*)"));
        assertTrue(matcher.match(attributes, "(attribute3=val^ue3)"));
        assertTrue(matcher.match(attributes, "(attribute3=*al^ue3)"));

        // The following should work: is this an issue with the old version of
        // the Apache Directory library we are using?
        // assertTrue(matcher.match(attributes, "(attribute4=val\\28ue\\294)"));
        // assertTrue(matcher.match(attributes, "(attribute5=value?)"));
        assertTrue(matcher.match(attributes, "(attribute6=value.+6)"));
        assertTrue(matcher.match(attributes, "(attribute6=*lue.+6)"));

        // check non-existing attributes
        assertFalse(matcher.match(attributes, "(nonexisting=value1)"));
        assertFalse(matcher.match(attributes, "(nonexisting=*)"));
    }

    @Test
    public void testCompoundFilter() throws Exception {
        AttributesImpl attributes = new AttributesImpl();
        attributes.put("attribute1", "value1");
        attributes.put("attribute2", "value2");

        LDAPFilterMatcher matcher = new LDAPFilterMatcher();

        // check negations
        assertTrue(matcher.match(attributes, "(!(attribute4=*))"));
        assertTrue(matcher.match(attributes, "(!(attribute1=valval*))"));
        assertTrue(matcher.match(attributes, "(!(attribute1=value2))"));

        // check conjunctions
        assertTrue(matcher.match(attributes, "(&(attribute1=*)(attribute2=*))"));
        assertTrue(matcher.match(attributes, "(&(attribute1=value1)(attribute2=value2))"));
        assertTrue(matcher.match(attributes, "(&(attribute1=value1)(attribute2=value2)(attribute1=val*))"));
        assertFalse(matcher.match(attributes, "(&(attribute1=value1)(attribute2=value3))"));
        assertFalse(matcher.match(attributes, "(&(attribute1=value1)(attribute2=value1))"));

        // check disjunctions
        assertTrue(matcher.match(attributes, "(|(attribute1=value2)(attribute2=*))"));
        assertTrue(matcher.match(attributes, "(|(attribute1=value2)(attribute2=value2))"));
        assertTrue(matcher.match(attributes, "(|(attribute1=value2)(attribute2=val*)(attribute1=value3))"));
        assertFalse(matcher.match(attributes, "(|(attribute1=value3)(attribute2=value3))"));

        // check nested boolean operator
        assertTrue(matcher.match(attributes, "(&(|(attribute1=value2)(attribute2=*))(attribute1=val*))"));
        assertTrue(matcher.match(attributes,
                "(&(|(attribute1=value2)(attribute2=*))(attribute1=val*)(!(attribute1=value2)))"));
    }

}
