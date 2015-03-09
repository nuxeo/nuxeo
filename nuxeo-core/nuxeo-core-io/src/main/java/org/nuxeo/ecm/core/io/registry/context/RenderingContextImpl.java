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

/*
 import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;
 import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;

 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CopyOnWriteArrayList;

 import org.nuxeo.common.utils.StringUtils;
 import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
 */

package org.nuxeo.ecm.core.io.registry.context;

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_ENRICHERS;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;
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

    private final Map<String, List<Object>> parameters = new ConcurrentHashMap<String, List<Object>>();

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

    @SuppressWarnings("deprecation")
    private Set<String> getSplittedParameterValues(String category, String... subCategories) {
        if (category == null) {
            return Collections.emptySet();
        }
        String paramKey = category;
        for (String subCategory : subCategories) {
            paramKey += "." + subCategory;
        }
        paramKey = paramKey.toLowerCase();
        List<Object> dirty = getParameters(paramKey);
        dirty.addAll(getParameters(HEADER_PREFIX + paramKey));
        // backward compatibility, supports X-NXDocumentProperties and X-NXContext-Category
        if (EMBED_PROPERTIES.toLowerCase().equals(paramKey)) {
            dirty.addAll(getParameters(MarshallingConstants.DOCUMENT_PROPERTIES_HEADER));
        } else if ((EMBED_ENRICHERS + "." + ENTITY_TYPE).toLowerCase().equals(paramKey)) {
            dirty.addAll(getParameters(MarshallingConstants.NXCONTENT_CATEGORY_HEADER));
        }
        Set<String> result = new TreeSet<String>();
        for (Object value : dirty) {
            if (value instanceof String && value != null) {
                for (String cleaned : org.nuxeo.common.utils.StringUtils.split((String) value, ',', true)) {
                    result.add(cleaned);
                }
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

    @Override
    public <T> List<T> getParameters(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String realName = name.toLowerCase().trim();
        @SuppressWarnings("unchecked")
        List<T> values = (List<T>) parameters.get(realName);
        if (values != null) {
            return new ArrayList<T>(values);
        } else {
            return new ArrayList<T>();
        }
    }

    @Override
    public Map<String, List<Object>> getAllParameters() {
        // make a copy of the local parameters
        Map<String, List<Object>> unModifiableParameters = new HashMap<String, List<Object>>();
        for (Map.Entry<String, List<Object>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            List<Object> value = entry.getValue();
            if (value == null) {
                unModifiableParameters.put(key, null);
            } else {
                unModifiableParameters.put(key, new ArrayList<Object>(value));
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
        parameters.put(realName, new CopyOnWriteArrayList<Object>(values));
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
        List<Object> currentValues = parameters.get(realName);
        if (currentValues == null) {
            currentValues = new CopyOnWriteArrayList<Object>();
            parameters.put(realName, currentValues);
        }
        for (Object value : values) {
            currentValues.add(value);
        }
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

        public RenderingContextBuilder fetch(String entityType, String... propertyName) {
            return paramValues(FETCH_PROPERTIES + "." + entityType, (Object[]) propertyName);
        }

        public RenderingContextBuilder translate(String entityType, String... propertyName) {
            return paramValues(TRANSLATE_PROPERTIES + "." + entityType, (Object[]) propertyName);
        }

        public RenderingContextBuilder fetchInDoc(String... propertyName) {
            return paramValues(FETCH_PROPERTIES + "." + ENTITY_TYPE, (Object[]) propertyName);
        }

        public RenderingContextBuilder enrichDoc(String... enricherName) {
            return paramValues(EMBED_ENRICHERS + "." + ENTITY_TYPE, (Object[]) enricherName);
        }

        public RenderingContextBuilder enrich(String entityType, String... enricherName) {
            return paramValues(EMBED_ENRICHERS + "." + ENTITY_TYPE, (Object[]) enricherName);
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
