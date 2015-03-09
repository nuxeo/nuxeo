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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContextImpl.RenderingContextBuilder;

/**
 * A context used to deliver parameter to marshallers during a marshalling request.
 * <p>
 * Use {@link CtxBuilder} to create your context.
 * </p>
 * <p>
 * When a {@link RenderingContext} is automatically provided from an HttpServletRequest, it contains request parameters,
 * headers and request attribute.
 * </p>
 * <p>
 * To get/set parameter values, use:
 * <ul>
 * <li>{@link RenderingContext#getParameter(String)}</li>
 * <li>{@link RenderingContext#getParameters(String)}</li>
 * <li>{@link RenderingContext#getBooleanParameter(String)}</li>
 * <li>{@link RenderingContext#getAllParameters()}</li>
 * <li>{@link RenderingContext#addParameterValues(String, Object...)}</li>
 * <li>{@link RenderingContext#addParameterListValues(String, List)}</li>
 * <li>{@link RenderingContext#setParameterValues(String, Object...)}</li>
 * <li>{@link RenderingContext#setParameterListValues(String, List)}</li>
 * </ul>
 * </p>
 * <p>
 * To manage document properties, entity enrichers or properties fetching, use:
 * <ul>
 * <li>{@link RenderingContext#getProperties()}</li>
 * <li>{@link RenderingContext#getEnrichers(String)}</li>
 * <li>{@link RenderingContext#getFetched(String)}</li>
 * </ul>
 * </p>
 * <p>
 * To manage infinite loop when calling a marshaller from another marshaller, use:
 * <ul>
 * <li>{@link RenderingContext#wrap()} -> {@link WrappedContext#controlDepth()}</li>
 * </ul>
 * Example:
 *
 * <pre>
 * // This will control infinite loop in this marshaller
 * try (Closeable resource = ctx.wrap().controlDepth().open()) {
 *     // call another marshaller to fetch the desired property here
 * } catch (MaxDepthReachedException mdre) {
 *     // do not call the other marshaller
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2.
 */
public interface RenderingContext {

    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    public static final String DEFAULT_URL = "http://fake-url.nuxeo.com/";

    /**
     * Gets the requested {@link Locale}.
     *
     * @since 7.2
     */
    public Locale getLocale();

    /**
     * Gets the current base url.
     *
     * @since 7.2
     */
    public String getBaseUrl();

    /**
     * Gets the current {@link CoreSession} or try to create one.
     *
     * @param document may be null, if present, this method search for a session in the document.
     * @return The current {@link CoreSession} if it exists. null otherwise.
     * @throws MarshallingException if no session could be created or found.
     * @since 7.2
     */
    public SessionWrapper getSession(DocumentModel document) throws MarshallingException;

    /**
     * Provides a {@link CoreSession} to marshallers.
     * <p>
     * For example: a {@link CoreSession} from the request context.
     * </p>
     *
     * @param session The existing {@link CoreSession} which lifecycle is managed outside the marshalling context.
     * @since 7.2
     */
    public void setExistingSession(CoreSession session);

    /**
     * Get all document properties. This will aggregate all values from parameters "properties", "X-NXproperties" and
     * "X-NXDocumentProperties". This supports value separated by comma.
     *
     * @return All document properties.
     * @since 7.2
     */
    public Set<String> getProperties();

    /**
     * Get all properties to fetch for a given entity type. This will aggregate all values from parameters
     * "fetch.entity" and "X-NXfetch.entity". This supports value separated by comma.
     *
     * @param entity The type of the entity on which you want to fetch properties.
     * @return All properties to fetch.
     * @since 7.2
     */
    public Set<String> getFetched(String entity);

    /**
     * Get all properties to translate for a given entity type. This will aggregate all values from parameters
     * "translate.entity" and "X-NXtranslate.entity". This supports value separated by comma.
     *
     * @param entity The type of the entity on which you want to fetch properties.
     * @return All properties to fetch.
     * @since 7.2
     */
    public Set<String> getTranslated(String entity);

    /**
     * Get all enrichers to activate on the given entity type. This will aggregate all values from parameters
     * "enrichers.entity", "X-NXenrichers.entity" and "X-NXContext-Category". This supports value separated by comma.
     *
     * @param entity The type of the entity on which you want to activate enrichers.
     * @return All enrichers to activate.
     * @since 7.2
     */
    public Set<String> getEnrichers(String entity);

    /**
     * see {@link WrappedContext}
     *
     * @return A new {@link WrappedContext}
     * @since 7.2
     */
    public WrappedContext wrap();

    /**
     * Get the casted parameter value for a given name. If multiple are available, the first found is returned.
     *
     * @param name The parameter name.
     * @return The first parameter value, null if no parameter are availble.
     * @since 7.2
     */
    public <T> T getParameter(String name);

    /**
     * see {@link #getParameter(String)}
     *
     * @return true is the parameter exists and if it's Boolean.TRUE or "true", false otherwise.
     */
    public boolean getBooleanParameter(String name);

    /**
     * Get the casted parameter values for a given name.
     *
     * @param name The parameter name.
     * @return The parameter values.
     * @since 7.2
     */
    public <T> List<T> getParameters(String name);

    /**
     * Get all parameter in this context except wrapped parameters.
     *
     * @return All parameter's names and their values.
     * @since 7.2
     */
    public Map<String, List<Object>> getAllParameters();

    /**
     * @see #setParameterListValues(String, List)
     * @since 7.2
     */
    public void setParameterValues(String name, Object... values);

    /**
     * Push values in the context with a given name. Please note that this method remove any value for the given name.
     *
     * @param name The parameter name.
     * @param values The parameter values.
     * @since 7.2
     */
    public void setParameterListValues(String name, List<Object> values);

    /**
     * @see #addParameterListValues(String, List)
     * @since 7.2
     */
    public void addParameterValues(String name, Object... values);

    /**
     * Add values in the context with a given name. Please note that this method keep current values for the given name.
     *
     * @param name The parameter name.
     * @param values The parameter values.
     * @since 7.2
     */
    public void addParameterListValues(String name, List<Object> values);

    /**
     * {@link RenderingContext} builder.
     * <p>
     * RenderingContext ctx = CtxBuilder.base("http://mine.nuxeo.com/nuxeo").locale(Locale.ENGLISH).param("name",
     * "value1", "value2").get();
     * </p>
     *
     * @since 7.2
     */
    public static final class CtxBuilder {
        private CtxBuilder() {
        }

        public static RenderingContextBuilder builder() {
            return new RenderingContextBuilder();
        }

        public static RenderingContextBuilder base(String url) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.base(url);
        }

        public static RenderingContextBuilder locale(Locale locale) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.locale(locale);
        }

        public static RenderingContextBuilder session(CoreSession session) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.session(session);
        }

        public static RenderingContextBuilder param(String name, Object value) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.param(name, value);
        }

        public static RenderingContextBuilder paramValues(String name, Object... values) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.paramValues(name, values);
        }

        public static RenderingContextBuilder paramList(String name, List<Object> values) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.paramList(name, values);
        }

        public static RenderingContextBuilder properties(String... schemaName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.properties(schemaName);
        }

        public static RenderingContextBuilder fetch(String entityType, String... propertyName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.fetch(entityType, propertyName);
        }

        public static RenderingContextBuilder fetchInDoc(String... propertyName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.fetchInDoc(propertyName);
        }

        public static RenderingContextBuilder translate(String entityType, String... propertyName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.translate(entityType, propertyName);
        }

        public static RenderingContextBuilder enrich(String entityType, String... enricherName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.enrich(entityType, enricherName);
        }

        public static RenderingContextBuilder enrichDoc(String... enricherName) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.enrichDoc(enricherName);
        }

        public static RenderingContextBuilder depth(DepthValues value) {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.depth(value);
        }

        public static RenderingContext get() {
            RenderingContextBuilder builder = new RenderingContextBuilder();
            return builder.get();
        }
    }

    /**
     * Session wrapper that manage the closing of new created session and preserve request scoped or document scoped
     * session.
     *
     * @since 7.2
     */
    public class SessionWrapper implements Closeable {

        private CoreSession session;

        private boolean shouldBeClosed;

        public SessionWrapper(CoreSession session, boolean shouldBeClosed) {
            super();
            this.session = session;
            this.shouldBeClosed = shouldBeClosed;
        }

        public CoreSession getSession() {
            return session;
        }

        public boolean shouldBeClosed() {
            return shouldBeClosed;
        }

        @Override
        public void close() throws IOException {
            if (shouldBeClosed) {
                session.close();
            }
        }

    }

}
