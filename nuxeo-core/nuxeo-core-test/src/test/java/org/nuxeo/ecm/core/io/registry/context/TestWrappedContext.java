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
