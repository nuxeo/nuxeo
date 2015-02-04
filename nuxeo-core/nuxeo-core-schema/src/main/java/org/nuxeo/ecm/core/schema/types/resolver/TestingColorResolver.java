package org.nuxeo.ecm.core.schema.types.resolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestingColorResolver implements ObjectResolver {

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

    private Map<String, Serializable> parameters;

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
        if (this.parameters != null) {
            throw new IllegalStateException("cannot change configuration, may be already in use somewhere");
        }
        String modeParam = parameters.get(COLOR_MODE);
        if (modeParam == null || modeParam.trim().isEmpty()) {
            throw new IllegalArgumentException("missing mode param");
        }
        try {
            mode = MODE.valueOf(modeParam);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("missing mode param", e);
        }
        this.parameters = new HashMap<String, Serializable>();
        this.parameters.put(COLOR_MODE, mode.name());
    }

    @Override
    public String getName() throws IllegalStateException {
        checkConfig();
        return NAME;
    }

    @Override
    public Map<String, Serializable> getParameters() throws IllegalStateException {
        checkConfig();
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean validate(Object value) throws IllegalStateException {
        checkConfig();
        return fetch(value) != null;
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

    private void checkConfig() throws IllegalStateException {
        if (parameters == null) {
            throw new IllegalStateException(
                    "you should call #configure(Map<String, String>) before. Please get this resolver throught ExternalReferenceService which is in charge of resolver configuration.");
        }
    }

}
