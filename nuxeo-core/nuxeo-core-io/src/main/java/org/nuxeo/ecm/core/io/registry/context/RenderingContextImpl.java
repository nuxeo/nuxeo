/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_ENRICHERS;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.SEPARATOR;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.TRANSLATE_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.WRAPPED_CONTEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.io.registry.MarshallingException;

/**
 * A thread-safe {@link RenderingContext} implementation. Please use {@link RenderingContext.CtxBuilder} to create
 * instance of {@link RenderingContext}.
 *
 * @since 7.2
 */
public class RenderingContextImpl implements RenderingContext {

    private String baseUrl = DEFAULT_URL;

    private Locale locale = DEFAULT_LOCALE;

    private CoreSession session = null;

    private final Map<String, List<Object>> parameters = new ConcurrentHashMap<>();

    private RenderingContextImpl() {
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public SessionWrapper getSession(DocumentModel document) {
        if (document != null) {
            CoreSession docSession = null;
            try {
                docSession = document.getCoreSession();
            } catch (UnsupportedOperationException e) {
                // do nothing
            }
            if (docSession != null) {
                return new SessionWrapper(docSession, false);
            }
        }
        if (session != null) {
            return new SessionWrapper(session, false);
        }
        String repoNameFound = getParameter("X-NXRepository");
        if (StringUtils.isBlank(repoNameFound)) {
            repoNameFound = getParameter("nxrepository");
            if (StringUtils.isBlank(repoNameFound)) {
                try {
                    repoNameFound = document.getRepositoryName();
                } catch (UnsupportedOperationException e) {
                    // do nothing
                }
            }
        }
        if (!StringUtils.isBlank(repoNameFound)) {
            CoreSession session = CoreInstance.openCoreSession(repoNameFound);
            return new SessionWrapper(session, true);
        }
        throw new MarshallingException("Unable to create a new session");
    }

    @Override
    public void setExistingSession(CoreSession session) {
        this.session = session;
    }

    @Override
    public Set<String> getProperties() {
        return getSplittedParameterValues(EMBED_PROPERTIES);
    }

    @Override
    public Set<String> getFetched(String entity) {
        return getSplittedParameterValues(FETCH_PROPERTIES, entity);
    }

    @Override
    public Set<String> getTranslated(String entity) {
        return getSplittedParameterValues(TRANSLATE_PROPERTIES, entity);
    }

    @Override
    public Set<String> getEnrichers(String entity) {
        return getSplittedParameterValues(EMBED_ENRICHERS, entity);
    }

    private Set<String> getSplittedParameterValues(String category, String... subCategories) {
        // supports dot '.' as separator
        Set<String> result = getSplittedParameterValues('.', category, subCategories);
        // supports hyphen '-' as separator
        result.addAll(getSplittedParameterValues(SEPARATOR, category, subCategories));
        return result;
    }

    @SuppressWarnings("deprecation")
    private Set<String> getSplittedParameterValues(char separator, String category, String... subCategories) {
        if (category == null) {
            return Collections.emptySet();
        }
        String paramKey = category;
        for (String subCategory : subCategories) {
            paramKey += separator + subCategory;
        }
        paramKey = paramKey.toLowerCase();
        List<Object> dirty = getParameters(paramKey);
        dirty.addAll(getParameters(HEADER_PREFIX + paramKey));
        // Deprecated on server since 5.8, but the code on client wasn't - keep this part of code as Nuxeo Automation
        // Client is deprecated since 8.10 and Nuxeo Java Client handle this properly
        // backward compatibility, supports X-NXDocumentProperties and X-NXContext-Category
        if (EMBED_PROPERTIES.toLowerCase().equals(paramKey)) {
            dirty.addAll(getParameters("X-NXDocumentProperties"));
        } else if ((EMBED_ENRICHERS + separator + ENTITY_TYPE).toLowerCase().equals(paramKey)) {
            dirty.addAll(getParameters("X-NXContext-Category"));
        }
        Set<String> result = new TreeSet<String>();
        for (Object value : dirty) {
            if (value instanceof String) {
                result.addAll(Arrays.asList(org.nuxeo.common.utils.StringUtils.split((String) value, ',', true)));
            }
        }
        return result;
    }

    private <T> T getWrappedEntity(String name) {
        return WrappedContext.getEntity(this, name);
    }

    @Override
    public WrappedContext wrap() {
        return WrappedContext.create(this);
    }

    @Override
    public <T> T getParameter(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String realName = name.toLowerCase().trim();
        List<Object> values = parameters.get(realName);
        if (values != null && values.size() > 0) {
            @SuppressWarnings("unchecked")
            T value = (T) values.get(0);
            return value;
        }
        if (WRAPPED_CONTEXT.toLowerCase().equals(realName)) {
            return null;
        } else {
            return getWrappedEntity(realName);
        }
    }

    @Override
    public boolean getBooleanParameter(String name) {
        Object result = getParameter(name);
        if (result == null) {
            return false;
        } else if (result instanceof Boolean) {
            return (Boolean) result;
        } else if (result instanceof String) {
            try {
                return Boolean.valueOf((String) result);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> List<T> getParameters(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String realName = name.toLowerCase().trim();
        List<T> values = (List<T>) parameters.get(realName);
        List<T> result;
        if (values != null) {
            result = new ArrayList<>(values);
        } else {
            result = new ArrayList<>();
        }
        if (WRAPPED_CONTEXT.toLowerCase().equals(realName)) {
            return result;
        } else {
            Object wrapped = getWrappedEntity(realName);
            if (wrapped == null) {
                return result;
            }
            if (wrapped instanceof List) {
                for (Object element : (List) wrapped) {
                    try {
                        T casted = (T) element;
                        result.add(casted);
                    } catch (ClassCastException e) {
                        return null;
                    }
                }
            } else {
                try {
                    T casted = (T) wrapped;
                    result.add(casted);
                } catch (ClassCastException e) {
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, List<Object>> getAllParameters() {
        // make a copy of the local parameters
        Map<String, List<Object>> unModifiableParameters = new HashMap<>();
        for (Map.Entry<String, List<Object>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            List<Object> value = entry.getValue();
            if (value == null) {
                unModifiableParameters.put(key, null);
            } else {
                unModifiableParameters.put(key, new ArrayList<>(value));
            }
        }
        return unModifiableParameters;
    }

    @Override
    public void setParameterValues(String name, Object... values) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        String realName = name.toLowerCase().trim();
        if (values.length == 0) {
            parameters.remove(realName);
            return;
        }
        setParameterListValues(realName, Arrays.asList(values));
    }

    @Override
    public void setParameterListValues(String name, List<Object> values) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        String realName = name.toLowerCase().trim();
        if (values == null) {
            parameters.remove(realName);
        }
        parameters.put(realName, new CopyOnWriteArrayList<>(values));
    }

    @Override
    public void addParameterValues(String name, Object... values) {
        addParameterListValues(name, Arrays.asList(values));
    }

    @Override
    public void addParameterListValues(String name, List<Object> values) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        String realName = name.toLowerCase().trim();
        if (values == null) {
            return;
        }
        parameters.computeIfAbsent(realName, key -> new CopyOnWriteArrayList()).addAll(values);
    }

    static RenderingContextBuilder builder() {
        return new RenderingContextBuilder();
    }

    public static final class RenderingContextBuilder {

        private RenderingContextImpl ctx;

        RenderingContextBuilder() {
            ctx = new RenderingContextImpl();
        }

        public RenderingContextBuilder base(String url) {
            ctx.baseUrl = url;
            return this;
        }

        public RenderingContextBuilder locale(Locale locale) {
            ctx.locale = locale;
            return this;
        }

        public RenderingContextBuilder session(CoreSession session) {
            ctx.session = session;
            return this;
        }

        public RenderingContextBuilder param(String name, Object value) {
            ctx.addParameterValues(name, value);
            return this;
        }

        public RenderingContextBuilder paramValues(String name, Object... values) {
            ctx.addParameterValues(name, values);
            return this;
        }

        public RenderingContextBuilder paramList(String name, List<Object> values) {
            ctx.addParameterListValues(name, values);
            return this;
        }

        public RenderingContextBuilder properties(String... schemaName) {
            return paramValues(EMBED_PROPERTIES, (Object[]) schemaName);
        }

        public RenderingContextBuilder enrich(String entityType, String... enricherName) {
            return paramValues(EMBED_ENRICHERS + SEPARATOR + entityType, (Object[]) enricherName);
        }

        public RenderingContextBuilder enrichDoc(String... enricherName) {
            return enrich(ENTITY_TYPE, enricherName);
        }

        public RenderingContextBuilder fetch(String entityType, String... propertyName) {
            return paramValues(FETCH_PROPERTIES + SEPARATOR + entityType, (Object[]) propertyName);
        }

        public RenderingContextBuilder fetchInDoc(String... propertyName) {
            return fetch(ENTITY_TYPE, propertyName);
        }

        public RenderingContextBuilder translate(String entityType, String... propertyName) {
            return paramValues(TRANSLATE_PROPERTIES + SEPARATOR + entityType, (Object[]) propertyName);
        }

        public RenderingContextBuilder depth(DepthValues value) {
            ctx.setParameterValues(MarshallingConstants.MAX_DEPTH_PARAM, value.name());
            return this;
        }

        public RenderingContext get() {
            return ctx;
        }

    }

}
