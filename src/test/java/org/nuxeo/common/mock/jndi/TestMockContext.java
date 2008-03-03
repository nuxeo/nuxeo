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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;

import junit.framework.TestCase;


/**
 * Tests all basic methods of MockContext.
 * Test for remote context lookup is not included.
 *
 * @author Dimitar Gospodinov
 */
public class TestMockContext extends TestCase {

    private Context initialCtx;
    private Context compCtx;
    private Context envCtx;
    private Context ejbCtx;

    @Override
    protected void setUp() throws Exception {
        MockContextFactory.setAsInitial();
        // Empty initial Context
        initialCtx = new InitialContext();
        initialCtx.bind("java:comp/env/ejb/Dummy", null);
        compCtx = (Context) initialCtx.lookup("java:comp");
        envCtx = (Context) compCtx.lookup("env");
        ejbCtx = (Context) envCtx.lookup("ejb");
    }

    @Override
    protected void tearDown() throws Exception {
        clearContext(initialCtx);
        ejbCtx = null;
        envCtx = null;
        compCtx = null;
        initialCtx = null;
        MockContextFactory.revertSetAsInitial();
    }

    /**
     * Removes all entries from the specified context, including subcontexts.
     *
     * @param context the context to clear
     */
    private static void clearContext(Context context) throws NamingException {
        for (NamingEnumeration<Binding> e = context.listBindings("");
            e.hasMoreElements();
           ) {
            Binding binding = e.nextElement();
            if (binding.getObject() instanceof Context) {
                clearContext((Context) binding.getObject());
            }
            context.unbind(binding.getName());
        }
    }

    /**
     * Tests inability to create duplicate subcontexts.
     *
     * @throws NamingException
     */
    public void testSubcontextCreationOfDuplicates() throws NamingException {
        // Try to create duplicate subcontext
        try {
            initialCtx.createSubcontext("java:comp");
            fail();
        } catch (NameAlreadyBoundException ex) { }
        // Try to create duplicate subcontext using multi-component name
        try {
            compCtx.createSubcontext("env/ejb");
            fail();
        } catch (NameAlreadyBoundException ex) { }
    }

    /**
     * Tests inability to destroy non-empty subcontexts.
     *
     * @throws NamingException
     */
    public void testSubcontextNonEmptyDestruction() throws NamingException {
        // Bind some object in ejb subcontext
        ejbCtx.bind("EmptyTest", "EmptyTest Object");
        // Attempt to destroy any subcontext
        try {
            initialCtx.destroySubcontext("java:comp");
            fail();
        } catch (ContextNotEmptyException ex) { }
        try {
            initialCtx.destroySubcontext("java:comp/env/ejb");
            fail();
        } catch (ContextNotEmptyException ex) { }
        try {
            envCtx.destroySubcontext("ejb");
            fail();
        } catch (ContextNotEmptyException ex) { }
    }

    /**
     * Tests ability to destroy empty subcontexts.
     *
     * @throws NamingException
     */
    public void testSubcontextDestruction() throws NamingException {
        // Create three new subcontexts
        ejbCtx.createSubcontext("sub1");
        ejbCtx.createSubcontext("sub2");
        envCtx.createSubcontext("sub3");
        // Destroy
        initialCtx.destroySubcontext("java:comp/env/ejb/sub1");
        ejbCtx.destroySubcontext("sub2");
        envCtx.destroySubcontext("sub3");
        // Perform lookup
        try {
            ejbCtx.lookup("sub1");
            fail();
        } catch (NameNotFoundException ex) { }
        try {
            envCtx.lookup("ejb/sub2");
            fail();
        } catch (NameNotFoundException ex) { }
        try {
            initialCtx.lookup("java:comp/sub3");
            fail();
        } catch (NameNotFoundException ex) { }
    }

    /**
     * Tests inability to invoke methods on destroyed subcontexts.
     *
     * @throws NamingException
     */
    public void testSubcontextInvokingMethodsOnDestroyedContext()
        throws NamingException {

        //Create subcontext and destroy it.
        Context sub = ejbCtx.createSubcontext("subXX");
        initialCtx.destroySubcontext("java:comp/env/ejb/subXX");
        /*
         * At this point sub is destroyed. Any method invokation should fail.
         * Try to bind some object, create subcontext and perform list.
         * Because the Context was empty, and non-empty Context can not be
         * destroyed, a lookup will fail and in general may not be tested.
         * We will test it for completness. The same applies for unbind and destroyContext methods.
         */
        try {
            sub.bind("SomeName", "SomeObject");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.unbind("SomeName");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.createSubcontext("SomeSubcontext");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.destroySubcontext("DummyName");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.list("");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.lookup("DummyName");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            sub.composeName("name", "prefix");
            fail();
        } catch (NoPermissionException ex) { }
        try {
            MockContextNameParser parser = new MockContextNameParser();
            sub.composeName(parser.parse("a"), parser.parse("b"));
            fail();
        } catch (NoPermissionException ex) { }
    }

    /**
     * Tests ability to bind name to object and inability to bind
     * duplicate names.
     * TODO Duplicate names can not be tested at this time because
     * we treat bind as re-bind.
     *
     * @throws NamingException
     */
    public void testBindLookup() throws NamingException {
        /*
         * Add four binding - two for null reference and two for an object,
         * using atomic and compound names.
         */
        Object o1 = "Test object for atomic binding";
        Object o2 = "Test object for compound binding";
        Object o3 = "Test object for complex compound binding";
        ejbCtx.bind("AtomicNull", null);
        ejbCtx.bind("AtomicObject", o1);
        initialCtx.bind("java:comp/env/CompoundNull", null);
        initialCtx.bind("java:comp/env/CompoundObject", o2);
        // Bind to subcontexts that do not exist
        initialCtx.bind("java:comp/env/ejb/subToCreate1/subToCreate2/oo", o3);

        // Try to lookup
        assertNull(ejbCtx.lookup("AtomicNull"));
        assertSame(ejbCtx.lookup("AtomicObject"), o1);
        assertNull(compCtx.lookup("env/CompoundNull"));
        assertSame(initialCtx.lookup("java:comp/env/CompoundObject"), o2);
        assertSame(ejbCtx.lookup("subToCreate1/subToCreate2/oo"), o3);
    }

    /**
     * Tests ability to unbind names.
     *
     * @throws NamingException
     */
    public void testUnbind() throws NamingException {
        envCtx.bind("testUnbindName1", null);
        compCtx.bind("env/ejb/testUnbindName2", "Test unbind object");
        // Unbind
        initialCtx.unbind("java:comp/env/testUnbindName1");
        ejbCtx.unbind("testUnbindName2");
        try {
            envCtx.lookup("testUnbindName1");
            fail();
        } catch (NameNotFoundException ex) { }
        try {
            initialCtx.lookup("java:comp/env/ejb/testUnbindName2");
            fail();
        } catch (NameNotFoundException ex) { }
        // Unbind non-existing name
        try {
            ejbCtx.unbind("This name does not exist in the context");
        } catch (Exception ex) {
            fail();
        }
        // Unbind non-existing name, when subcontext does not exists
        try {
            compCtx.unbind("env/ejb/ejb1/somename");
            fail();
        } catch (NameNotFoundException ex) { }
    }

    /**
     * Tests ability to list bindings for a context - specified by
     * name through object reference.
     *
     * @throws NamingException
     */
    public void testListBindings() throws NamingException {
        // Add three bindings
        Object o1 = "Test list bindings 1";
        Object o2 = "Test list bindings 2";

        compCtx.bind("env/ejb/testListBindings1", o1);
        envCtx.bind("testListBindings2", o2);
        ejbCtx.bind("testListBindings3", null);

        // Verify bindings for context specified by reference
        verifyListBindingsResult(envCtx, "", o1, o2);
        // Verify bindings for context specified by name
        verifyListBindingsResult(initialCtx, "java:comp/env", o1, o2);
    }

    private void verifyListBindingsResult(Context c, String name, Object o1,
            Object o2) throws NamingException {
        boolean ejbFoundFlg = false;
        boolean o2FoundFlg = false;
        boolean ejbO1FoundFlg = false;
        boolean ejbNullFoundFlg = false;

        // List bindings for the specified context
        for (NamingEnumeration<Binding> en = c.listBindings(name); en.hasMore();) {
            Binding b = en.next();
            if (b.getName().equals("ejb")) {
                assertEquals(b.getObject(), ejbCtx);
                ejbFoundFlg = true;

                Context nextCon = (Context) b.getObject();
                for (NamingEnumeration<Binding> en1 = nextCon.listBindings(""); en1.hasMore();) {
                    Binding b1 = en1.next();
                    if (b1.getName().equals("testListBindings1")) {
                        assertEquals(b1.getObject(), o1);
                        ejbO1FoundFlg = true;
                    } else if (b1.getName().equals("testListBindings3")) {
                        assertNull(b1.getObject());
                        ejbNullFoundFlg = true;
                    }
                }
            } else if (b.getName().equals("testListBindings2")) {
                assertEquals(b.getObject(), o2);
                o2FoundFlg = true;
            }
        }
        if (!(ejbFoundFlg && o2FoundFlg && ejbO1FoundFlg && ejbNullFoundFlg)) {
            fail();
        }
    }

    public void testCompositeNameWithLeadingTrailingEmptyComponents()
            throws NamingException {
        MockContext c = new MockContext(null);
        Object o = new Object();

        c.rebind("/a/b/c/", o);
        assertEquals(c.lookup("a/b/c"), o);
        assertEquals(c.lookup("///a/b/c///"), o);
    }

    public void testLookup() throws NamingException {
        MockContext mockCtx = new MockContext(null);
        Object obj = new Object();
        mockCtx.rebind("a/b/c/d", obj);
        assertEquals(obj, mockCtx.lookup("a/b/c/d"));

        mockCtx.bind("a", obj);
        assertEquals(obj, mockCtx.lookup("a"));
    }

    public void testGetCompositeName() throws NamingException {
        MockContext mockCtx = new MockContext(null);
        mockCtx.rebind("a/b/c/d", new Object());

        MockContext subCtx;

        subCtx = (MockContext) mockCtx.lookup("a");
        assertEquals("a", subCtx.getCompoundStringName());

        subCtx = (MockContext) mockCtx.lookup("a/b/c");
        assertEquals("a/b/c", subCtx.getCompoundStringName());
    }

    /**
     * Tests that delegate context is
     * invoked when MockContext does not find the name.
     */
    public void testDelegateContext() throws NamingException {
        List<String> recordedLookups = new ArrayList<String>();
        Context ctx = new MockContext(new RecordingMockContext(recordedLookups));

        // Test simple name
        String wrongName = "mockejb";
        ctx.lookup(wrongName);
        assertEquals(1, recordedLookups.size());
        assertEquals(wrongName, recordedLookups.get(0));

        // Test composite name
        recordedLookups.clear();
        wrongName = "mockejb/a";
        ctx.lookup(wrongName);
        assertEquals(1, recordedLookups.size());
        assertEquals(wrongName, recordedLookups.get(0));

        // Test the situation when root context is bound already in MockCOntext
        recordedLookups.clear();
        ctx.rebind("mockejb/dummy", new Object());
        wrongName = "mockejb/a";
        ctx.lookup(wrongName);
        assertEquals(1, recordedLookups.size());
        assertEquals(wrongName, recordedLookups.get(0));
    }

    /**
     * Tests substitution of '.' with '/' when parsing string names.
     *
     * @throws NamingException
     */
    public void testTwoSeparatorNames() throws NamingException {
        MockContext ctx = new MockContext(null);
        Object obj = new Object();

        ctx.bind("a/b.c.d/e", obj);
        assertEquals(ctx.lookup("a/b/c/d/e"), obj);
        assertEquals(ctx.lookup("a.b/c.d.e"), obj);
        assertTrue(ctx.lookup("a.b.c.d") instanceof Context);
    }

    /**
     * Class to simulate remote context.
     * Always returns the dummy object from lookup
     * and stores the lookup call info.
     */
    static class RecordingMockContext extends MockContext {

        private final Collection<String> recordedLookups;

        RecordingMockContext(Collection<String> recordedNames) {
            super(null);
            recordedLookups = recordedNames;
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            recordedLookups.add(name.toString());
            return new Object();
        }

        @Override
        public Object lookup(String name) throws NamingException {
            recordedLookups.add(name);
            return new Object();
        }

    }

}
