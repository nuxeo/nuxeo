/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintUtils;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;

/**
 * The implementation for a field, which is the association of a type, a name, and default values.
 */
public class FieldImpl implements Field {

    private static final long serialVersionUID = 1L;

    private QName name;

    private Type type;

    private Type declaringType;

    private int flags;

    private int maxOccurs = 1;

    private int minOccurs = 1;

    private String defaultValue;

    private int maxLength = -1;

    private Set<Constraint> constraints;

    private String fallbackXpath;

    public FieldImpl(QName name, Type declaringType, Type type, String defaultValue, int flags,
            Collection<Constraint> constraints) {
        this.name = name;
        this.type = type;
        this.declaringType = declaringType;
        this.defaultValue = defaultValue;
        this.flags = flags;
        this.constraints = new HashSet<Constraint>();
        if (constraints != null) {
            this.constraints.addAll(constraints);
        }
    }

    public FieldImpl(QName name, Type declaringType, Type type) {
        this(name, declaringType, type, null, 0, new ArrayList<Constraint>());
    }

    public FieldImpl(QName name, Type declaringType, Type type, String defaultValue, int flags) {
        this(name, declaringType, type, defaultValue, flags, new ArrayList<Constraint>());
    }

    @Override
    public Type getDeclaringType() {
        return declaringType;
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getDefaultValue() {
        return type.decode(defaultValue);
    }

    @Override
    public boolean isNillable() {
        return ConstraintUtils.getConstraint(constraints, NotNullConstraint.class) == null;
    }

    @Override
    public boolean isConstant() {
        return (flags & CONSTANT) != 0;
    }

    @Override
    public boolean isDeprecated() {
        return (flags & DEPRECATED) != 0;
    }

    @Override
    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    @Override
    public void setNillable(boolean isNillable) {
        if (isNillable) {
            flags |= NILLABLE;
        } else {
            flags &= ~NILLABLE;
        }
    }

    @Override
    public void setConstant(boolean isConstant) {
        if (isConstant) {
            flags |= CONSTANT;
        } else {
            flags &= ~CONSTANT;
        }
    }

    @Override
    public void setDeprecated(boolean isDeprecated) {
        if (isDeprecated) {
            flags |= DEPRECATED;
        } else {
            flags &= ~DEPRECATED;
        }
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public void setMaxOccurs(int max) {
        maxOccurs = max;
    }

    @Override
    public void setMinOccurs(int min) {
        minOccurs = min;
    }

    @Override
    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void setMaxLength(int length) {
        maxLength = length;
    }

    @Override
    public String toString() {
        return name + " [" + type.getName() + ']';
    }

    @Override
    public Set<Constraint> getConstraints() {
        return Collections.unmodifiableSet(constraints);
    }

    public String getFallbackXpath() {
        return fallbackXpath;
    }

    public void setFallbackXpath(String fallbackXpath) {
        this.fallbackXpath = fallbackXpath;
    }

}
