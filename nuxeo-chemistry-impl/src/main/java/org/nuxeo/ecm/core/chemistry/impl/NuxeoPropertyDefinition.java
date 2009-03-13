/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.chemistry.property.Choice;
import org.apache.chemistry.property.PropertyDefinition;
import org.apache.chemistry.property.PropertyType;
import org.apache.chemistry.property.Updatability;

public class NuxeoPropertyDefinition implements PropertyDefinition {

    private final String name;

    private final String id;

    private final String displayName;

    private final String description;

    private final boolean inherited;

    private final PropertyType type;

    private final boolean multiValued;

    private final List<Choice> choices;

    private final boolean openChoice;

    private final boolean required;

    private final Serializable defaultValue;

    private final Updatability updatability;

    private final boolean queryable;

    private final boolean orderable;

    private final int precision;

    private final Integer minValue;

    private final Integer maxValue;

    private final int maxLength;

    private final URI schemaURI;

    private final String encoding;

    public NuxeoPropertyDefinition(String name, String id, String displayName,
            String description, boolean inherited, PropertyType type,
            boolean multiValued, List<Choice> choices, boolean openChoice,
            boolean required, Serializable defaultValue,
            Updatability updatability, boolean queryable, boolean orderable,
            int precision, Integer minValue, Integer maxValue, int maxLength,
            URI schemaURI, String encoding) {
        super();
        this.name = name;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.inherited = inherited;
        this.type = type;
        this.multiValued = multiValued;
        this.choices = choices == null ? null
                : Collections.unmodifiableList(choices);
        this.openChoice = openChoice;
        this.required = required;
        this.defaultValue = defaultValue;
        this.updatability = updatability;
        this.queryable = queryable;
        this.orderable = orderable;
        this.precision = precision;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.maxLength = maxLength;
        this.schemaURI = schemaURI;
        this.encoding = encoding;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isInherited() {
        return inherited;
    }

    public PropertyType getType() {
        return type;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public boolean isOpenChoice() {
        return openChoice;
    }

    public boolean isRequired() {
        return required;
    }

    public Serializable getDefaultValue() {
        return defaultValue;
    }

    public Updatability getUpdatability() {
        return updatability;
    }

    public boolean isQueryable() {
        return queryable;
    }

    public boolean isOrderable() {
        return orderable;
    }

    public int getPrecision() {
        return precision;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public URI getSchemaURI() {
        return schemaURI;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean validates(Serializable value) {
        return validationError(value) == null;
    }

    public String validationError(Serializable value) {
        if (getUpdatability() == Updatability.READ_ONLY) {
            // TODO Updatability.WHEN_CHECKED_OUT
            return "Property is read-only";
        }
        if (value == null) {
            if (isRequired()) {
                return "Property is required";
            }
            return null;
        }
        boolean multi = isMultiValued();
        if (multi != value.getClass().isArray()) {
            return multi ? "Property is multi-valued"
                    : "Property is single-valued";
        }
        Class<?> klass;
        switch (type) {
        case STRING:
        case ID:
            klass = String.class;
            break;
        case DECIMAL:
            klass = BigDecimal.class;
            break;
        case INTEGER:
            klass = Integer.class; // TODO Long
            break;
        case BOOLEAN:
            klass = Boolean.class;
            break;
        case DATETIME:
            klass = Calendar.class;
            break;
        case URI:
            klass = URI.class;
            break;
        case XML:
            klass = String.class; // TODO
            break;
        case HTML:
            klass = String.class; // TODO
            break;
        default:
            throw new UnsupportedOperationException(type.toString());
        }
        if (multi) {
            for (int i = 0; i < Array.getLength(value); i++) {
                Object v = Array.get(value, i);
                if (v == null) {
                    return "Array value cannot contain null elements";
                }
                if (!klass.isInstance(v)) {
                    return "Array value has type " + v.getClass()
                            + " instead of " + klass.getName();
                }
            }
        } else {
            if (!klass.isInstance(value)) {
                return "Value has type " + value.getClass() + " instead of "
                        + klass.getName();
            }
        }
        return null;
    }

}
