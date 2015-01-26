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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.DEPTH_CONTROL_KEY_PREFIX;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.WRAPPED_CONTEXT;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.io.registry.MarshallingException;

/**
 * Provides a way to create sub contexts of {@link RenderingContext} to broadcast marshalled entities or state
 * parameters from a marshaller to other marshaller.
 * <p>
 * First, create the context, fill it and then use a try-resource statement to ensure the context will be closed.
 * </p>
 *
 * <pre>
 * <code>
 * DocumentModel doc = ...;
 * RenderingContext ctx = ...;
 * try (Closeable ctx = ctx.wrap().with(ENTITY_DOCUMENT, doc).open()) {
 *   // the document will only be available in the following statements
 *   // call other marshallers here and put this code the get the doc : DocumentModel contextualDocument = ctx.getParameter(ENTITY_DOCUMENT);
 * }
 * </code>
 * </pre>
 * <p>
 * Note that if in the try-resource statement, another context is created, the entity will be searched first in this
 * context, if not found, recursively in the parent context: the nearest will be returned.
 * </p>
 *
 * @since 7.2
 */
public final class WrappedContext {

    private WrappedContext parent;

    private RenderingContext ctx;

    private Map<String, Object> entries = new HashMap<String, Object>();

    private WrappedContext(RenderingContext ctx) {
        if (ctx == null) {
            throw new MarshallingException("Cannot get a wrapped context without RenderingContext");
        }
        this.ctx = ctx;
        parent = ctx.getParameter(WRAPPED_CONTEXT);
    }

    private static WrappedContext get(RenderingContext ctx) {
        if (ctx != null) {
            return ctx.getParameter(WRAPPED_CONTEXT);
        } else {
            throw new MarshallingException("Cannot get a wrapped context without RenderingContext");
        }
    }

    /**
     * Creates a new WrappedContext in the given {@link RenderingContext}.
     *
     * @param ctx The {@link RenderingContext} where this {@link WrappedContext} will be available.
     * @return The created {@link WrappedContext}.
     * @since 7.2
     */
    static WrappedContext create(RenderingContext ctx) {
        if (ctx != null) {
            WrappedContext child = new WrappedContext(ctx);
            return child;
        } else {
            throw new MarshallingException("Cannot get a wrapped context without RenderingContext");
        }
    }

    /**
     * Push a value in this the context.
     *
     * @param key The string used to get the entity.
     * @param value The value to push.
     * @return this {@link WrappedContext}.
     * @since 7.2
     */
    public final WrappedContext with(String key, Object value) {
        if (StringUtils.isEmpty(key)) {
            return this;
        }
        String realKey = key.toLowerCase().trim();
        entries.put(realKey, value);
        return this;
    }

    /**
     * Call this method to avoid an infinite loop while calling a marshaller from another.
     * <p>
     * This method increases the current number of "marshaller-to-marshaller" calls. And then checks that this number do
     * not exceed the "depth" parameter. If the "depth" parameter is not provided or if it's not valid, the default
     * value is "root" (expected valid values are "root", "children" or "max" - see {@link DepthValues}).
     * </p>
     * <p>
     * Here is the prettiest way to write it:
     *
     * <pre>
     * // This will control infinite loop in this marshaller
     * try (Closeable resource = ctx.wrap().controlDepth().open()) {
     *     // call another marshaller to fetch the desired property here
     * } catch (MaxDepthReachedException e) {
     *     // do not call the other marshaller
     * }
     * </pre>
     *
     * </p>
     * <p>
     * You can also control the depth before (usefull for list):
     *
     * <pre>
     * try {
     *     WrappedContext wrappedCtx = ctx.wrap().controlDepth();
     *     // prepare your calls
     *     ...
     *     // This will control infinite loop in this marshaller
     *     try (Closeable resource = wrappedCtx.open()) {
     *         // call another marshaller to fetch the desired property here
     *     }
     * } catch (MaxDepthReachedException e) {
     *     // manage the case
     * }
     * </pre>
     *
     * </p>
     *
     * @return
     * @throws MaxDepthReachedException
     * @since TODO
     */
    public final WrappedContext controlDepth() throws MaxDepthReachedException {
        String depthKey = DEPTH_CONTROL_KEY_PREFIX + "DEFAULT";
        Integer value = getEntity(ctx, depthKey);
        Integer maxDepth;
        try {
            maxDepth = DepthValues.valueOf(ctx.getParameter("depth")).getDepth();
        } catch (IllegalArgumentException | NullPointerException e) {
            maxDepth = DepthValues.root.getDepth();
        }
        if (value == null) {
            value = 0;
        }
        value++;
        if (value > maxDepth) {
            throw new MaxDepthReachedException();
        }
        entries.put(depthKey.toLowerCase(), value);
        return this;
    }

    /**
     * Provides a flatten map of wrapped contexts. If a same entity type is stored in multiple contexts, the nearest one
     * will be returned.
     *
     * @since 7.2
     */
    public final Map<String, Object> flatten() {
        Map<String, Object> mergedResult = new HashMap<String, Object>();
        if (parent != null) {
            mergedResult.putAll(parent.flatten());
        }
        mergedResult.putAll(entries);
        return mergedResult;
    }

    /**
     * Gets the nearest value stored in the {@link WrappedContext}.
     *
     * @param ctx The {@link RenderingContext} in which the value will be searched.
     * @param key The key used to store the value in the context.
     * @return The casted entity.
     * @since 7.2
     */
    static <T> T getEntity(RenderingContext ctx, String key) {
        T value = null;
        WrappedContext wrappedCtx = get(ctx);
        if (wrappedCtx != null) {
            if (StringUtils.isEmpty(key)) {
                return null;
            }
            String realKey = key.toLowerCase().trim();
            return wrappedCtx.innerGetEntity(realKey);
        }
        return value;
    }

    /**
     * Recursive search for the nearest entity.
     *
     * @since 7.2
     */
    private final <T> T innerGetEntity(String entityType) {
        @SuppressWarnings("unchecked")
        T value = (T) entries.get(entityType);
        if (value == null && parent != null) {
            return parent.innerGetEntity(entityType);
        }
        return value;
    }

    /**
     * Open the context and make all embedded entities available. Returns a {@link Closeable} which must be closed at
     * the end.
     * <p>
     * Note the same context could be opened and closed several times.
     * </p>
     *
     * @return A {@link Closeable} instance.
     * @since 7.2
     */
    public final Closeable open() {
        ctx.setParameterValues(WRAPPED_CONTEXT, this);
        return new Closeable() {
            @Override
            public void close() throws IOException {
                ctx.setParameterValues(WRAPPED_CONTEXT, parent);
            }
        };
    }

    /**
     * Prints this context.
     */
    @Override
    public String toString() {
        return flatten().toString();
    }

}
