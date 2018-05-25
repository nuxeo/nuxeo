/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.schema.types.resolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestingColorResolver extends AbstractObjectResolver implements ObjectResolver {

    public static enum MODE {
        PRIMARY, SECONDARY;
    }

    public interface Color {
        String name();
    }

    public static enum PrimaryColor implements Color {
        RED, BLUE, YELLOW;
    }

    public static enum SecondaryColor implements Color {
        VIOLET, ORANGE, GREEN;
    }

    public static final String COLOR_MODE = "mode";

    public static final String NAME = "colorReference";

    private MODE mode;

    private List<Class<?>> managedClasses = null;

    @Override
    public List<Class<?>> getManagedClasses() {
        if (managedClasses == null) {
            managedClasses = new ArrayList<Class<?>>();
            managedClasses.add(Color.class);
        }
        return managedClasses;
    }

    @Override
    public void configure(Map<String, String> parameters) throws IllegalStateException, IllegalArgumentException {
        super.configure(parameters);
        String modeParam = parameters.get(COLOR_MODE);
        if (modeParam == null || modeParam.trim().isEmpty()) {
            throw new IllegalArgumentException("missing mode param");
        }
        try {
            mode = MODE.valueOf(modeParam);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("missing mode param", e);
        }
        this.parameters.put(COLOR_MODE, mode.name());
    }

    @Override
    public String getName() throws IllegalStateException {
        checkConfig();
        return NAME;
    }

    @Override
    public Color fetch(Object value) throws IllegalStateException {
        checkConfig();
        if (value instanceof String) {
            String ref = (String) value;
            switch (mode) {
            case PRIMARY:
                for (PrimaryColor color : PrimaryColor.values()) {
                    if (color.name().equals(ref)) {
                        return color;
                    }
                }
                break;
            case SECONDARY:
                for (SecondaryColor color : SecondaryColor.values()) {
                    if (color.name().equals(ref)) {
                        return color;
                    }
                }
                break;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fetch(Class<T> type, Object value) throws IllegalStateException {
        checkConfig();
        if (Color.class.equals(type)) {
            return (T) fetch(value);
        } else if (mode == MODE.PRIMARY && PrimaryColor.class.equals(type)) {
            return (T) fetch(value);
        } else if (mode == MODE.SECONDARY && SecondaryColor.class.equals(type)) {
            return (T) fetch(value);
        }
        return null;
    }

    @Override
    public Serializable getReference(Object entity) throws IllegalStateException {
        checkConfig();
        if (entity instanceof Color) {
            Color color = (Color) entity;
            if (color != null) {
                switch (mode) {
                case PRIMARY:
                    if (color instanceof PrimaryColor) {
                        return color.name();
                    }
                    break;
                case SECONDARY:
                    if (color instanceof SecondaryColor) {
                        return color.name();
                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        checkConfig();
        return String.format("\"%s\" is not a correct %s color", invalidValue, mode.name().toLowerCase());
    }

}
