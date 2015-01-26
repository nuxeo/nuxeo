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

package org.nuxeo.ecm.core.io.registry.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;

public class TestWrappedContext {

    private static final String VALUE1 = "value1";

    private static final String VALUE2 = "value2";

    private static final String PARAM = "test";

    @Test
    public void noContext() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        assertNull(ctx.getParameter(PARAM));
    }

    @Test
    public void emptyContext() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try (Closeable wrapped = ctx.wrap().open()) {
            assertNull(ctx.getParameter(PARAM));
        }
    }

    @Test
    public void simpleParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try (Closeable wrapped = ctx.wrap().with(PARAM, VALUE1).open()) {
            Object value = ctx.getParameter(PARAM);
            assertNotNull(value);
            assertEquals(VALUE1, value);
        }
    }

    @Test
    public void subContext() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try (Closeable wrapped = ctx.wrap().with(PARAM, VALUE1).open()) {
            Object value = ctx.getParameter(PARAM);
            assertNotNull(value);
            assertEquals(VALUE1, value);
            try (Closeable wrapped2 = ctx.wrap().with(PARAM, VALUE2).open()) {
                value = ctx.getParameter(PARAM);
                assertNotNull(value);
                assertEquals(VALUE2, value);
            }
            value = ctx.getParameter(PARAM);
            assertNotNull(value);
            assertEquals(VALUE1, value);
        }
    }

    @Test
    public void basicParameterOverridesWrapped() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(PARAM, VALUE2).get();
        try (Closeable wrapped = ctx.wrap().with(PARAM, VALUE1).open()) {
            Object value = ctx.getParameter(PARAM);
            assertNotNull(value);
            assertEquals(VALUE2, value);
        }
    }

    @Test
    public void testControlDepthDefaultIsRoot() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try (Closeable wrappedRoot = ctx.wrap().controlDepth().open()) {
            try (Closeable wrappedChild = ctx.wrap().controlDepth().open()) {
                fail();
            } catch (MaxDepthReachedException mdre) {
                // ok
            }
        } catch (MaxDepthReachedException mdre) {
            fail();
        }
    }

    @Test
    public void testRootControlDepth() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.depth(DepthValues.root).get();
        try (Closeable wrappedRoot = ctx.wrap().controlDepth().open()) {
            try (Closeable wrappedChild = ctx.wrap().controlDepth().open()) {
                fail();
            } catch (MaxDepthReachedException mdre) {
                // ok
            }
        } catch (MaxDepthReachedException mdre) {
            fail();
        }
    }

    @Test
    public void testChildControlDepth() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.depth(DepthValues.children).get();
        try (Closeable wrappedRoot = ctx.wrap().controlDepth().open()) {
            try (Closeable wrappedChild = ctx.wrap().controlDepth().open()) {
                try (Closeable wrappedMax = ctx.wrap().controlDepth().open()) {
                    fail();
                } catch (MaxDepthReachedException mdre) {
                    // ok
                }
            } catch (MaxDepthReachedException mdre) {
                fail();
            }
        } catch (MaxDepthReachedException mdre) {
            fail();
        }
    }

    @Test
    public void testMaxControlDepth() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.depth(DepthValues.max).get();
        try (Closeable wrappedRoot = ctx.wrap().controlDepth().open()) {
            try (Closeable wrappedChild = ctx.wrap().controlDepth().open()) {
                try (Closeable wrappedMax = ctx.wrap().controlDepth().open()) {
                    try (Closeable wrappedOver = ctx.wrap().controlDepth().open()) {
                        fail();
                    } catch (MaxDepthReachedException mdre) {
                        // ok
                    }
                } catch (MaxDepthReachedException mdre) {
                    fail();
                }
            } catch (MaxDepthReachedException mdre) {
                fail();
            }
        } catch (MaxDepthReachedException mdre) {
            fail();
        }
    }

}
