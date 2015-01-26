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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.reflect.Instantiations;

/**
 * A {@link ThreadLocal} delegate {@link RenderingContext} used for {@link Instantiations#SINGLETON} marshallers.
 *
 * @since 7.2
 */
public final class ThreadSafeRenderingContext implements RenderingContext {

    private final ThreadLocal<RenderingContext> ctx = new ThreadLocal<RenderingContext>();

    public ThreadSafeRenderingContext() {
    }

    /**
     * Configure this {@link RenderingContext} to use the given {@link RenderingContext} in the current thread.
     *
     * @param delegates The underlying {@link RenderingContext}.
     * @since 7.2
     */
    public void configureThread(RenderingContext delegates) {
        ctx.set(delegates);
    }

    /**
     * Get the underlying {@link RenderingContext}.
     *
     * @return The underlying {@link RenderingContext}.
     * @since 7.2
     */
    public RenderingContext getDelegate() {
        return ctx.get();
    }

    @Override
    public Locale getLocale() {
        return ctx.get().getLocale();
    }

    @Override
    public String getBaseUrl() {
        return ctx.get().getBaseUrl();
    }

    @Override
    public SessionWrapper getSession(DocumentModel document) {
        return ctx.get().getSession(document);
    }

    @Override
    public void setExistingSession(CoreSession session) {
        ctx.get().setExistingSession(session);
    }

    @Override
    public Set<String> getProperties() {
        return ctx.get().getProperties();
    }

    @Override
    public Set<String> getFetched(String entity) {
        return ctx.get().getFetched(entity);
    }

    @Override
    public Set<String> getEnrichers(String entity) {
        return ctx.get().getEnrichers(entity);
    }

    @Override
    public WrappedContext wrap() {
        return ctx.get().wrap();
    }

    @Override
    public <T> T getParameter(String name) {
        return ctx.get().getParameter(name);
    }

    @Override
    public boolean getBooleanParameter(String name) {
        return ctx.get().getBooleanParameter(name);
    }

    @Override
    public <T> List<T> getParameters(String name) {
        return ctx.get().getParameters(name);
    }

    @Override
    public Map<String, List<Object>> getAllParameters() {
        return ctx.get().getAllParameters();
    }

    @Override
    public void setParameterValues(String name, Object... values) {
        ctx.get().setParameterValues(name, values);
    }

    @Override
    public void setParameterListValues(String name, List<Object> values) {
        ctx.get().setParameterListValues(name, values);
    }

    @Override
    public void addParameterValues(String name, Object... values) {
        ctx.get().addParameterValues(name, values);
    }

    @Override
    public void addParameterListValues(String name, List<Object> values) {
        ctx.get().addParameterListValues(name, values);
    }

}
