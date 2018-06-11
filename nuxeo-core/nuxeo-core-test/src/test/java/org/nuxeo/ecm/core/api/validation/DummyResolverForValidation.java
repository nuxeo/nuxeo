/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.validation;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.resolver.AbstractObjectResolver;

/**
 * Dummy resolver which just wrap the input value with {@link Dummy}.
 * <p/>
 * Resolver also has {@link #PARAM_FETCH_IT} parameter in order to not return null instead of dummy wrapper. This also
 * makes the property always invalid.
 */
public class DummyResolverForValidation extends AbstractObjectResolver {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "dummyResolver";

    protected static final String PARAM_FETCH_IT = "fetchIt";

    private boolean fetchIt;

    @Override
    public void configure(Map<String, String> parameters) throws IllegalArgumentException, IllegalStateException {
        super.configure(parameters);
        String paramFetchIt = parameters.get(PARAM_FETCH_IT);
        this.parameters.put(PARAM_FETCH_IT, paramFetchIt);
        fetchIt = Boolean.parseBoolean(paramFetchIt);
    }

    @Override
    public List<Class<?>> getManagedClasses() {
        return Collections.singletonList(Dummy.class);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object fetch(Object value) {
        checkConfig();
        if (fetchIt) {
            return new Dummy((String) value);
        }
        return null;
    }

    @Override
    public <T> T fetch(Class<T> type, Object value) {
        Object dummy = fetch(value);
        if (type.isInstance(dummy)) {
            return type.cast(dummy);
        }
        return null;
    }

    @Override
    public Serializable getReference(Object object) {
        if (object instanceof Dummy) {
            return ((Dummy) object).getReference();
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        return "'" + invalidValue + "' is not valid";
    }

    public static class Dummy {

        protected final String reference;

        public Dummy(String reference) {
            this.reference = reference;
        }

        public String getReference() {
            return reference;
        }
    }
}
