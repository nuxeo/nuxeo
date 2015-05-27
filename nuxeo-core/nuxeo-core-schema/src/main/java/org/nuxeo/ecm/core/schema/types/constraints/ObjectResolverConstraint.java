/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * External references are document field with a simple type whose value refers to an external business entity. This
 * constraints ensure some value is a reference of an existing external entity resolved by the underlying resolver :
 * {@link #getResolver()} .
 *
 * @since 7.1
 */
public final class ObjectResolverConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 1L;

    private ObjectResolver resolver;

    public ObjectResolverConstraint(ObjectResolver resolver) {
        super();
        this.resolver = resolver;
    }

    public ObjectResolver getResolver() {
        return resolver;
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return true;
        }
        return resolver.validate(object);
    }

    @Override
    public Description getDescription() {
        Map<String, Serializable> parameters = Collections.unmodifiableMap(resolver.getParameters());
        return new Description(resolver.getName(), parameters);
    }

    @Override
    public String getErrorMessage(Object invalidValue, Locale locale) {
        return resolver.getConstraintErrorMessage(invalidValue, locale);
    }
}
