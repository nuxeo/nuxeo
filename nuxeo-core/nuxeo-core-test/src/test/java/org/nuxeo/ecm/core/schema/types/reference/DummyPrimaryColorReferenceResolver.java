package org.nuxeo.ecm.core.schema.types.reference;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.reference.DummyPrimaryColorReferenceResolver.Color;

public class DummyPrimaryColorReferenceResolver implements ExternalReferenceResolver<Color> {

    public static final String NAME = "DummyColorReference";

    private Map<String, Serializable> parameters;

    public static enum Color {
        RED, GREEN, BLUE;
    }

    @Override
    public void configure(Map<String, String> parameters) throws IllegalArgumentException {
        this.parameters = new HashMap<String, Serializable>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            this.parameters.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Serializable> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public Class<?> getEntityTypes() {
        return Color.class;
    }

    @Override
    public boolean validate(Object value) throws IllegalStateException {
        return fetch(value) != null;
    }

    @Override
    public Color fetch(Object value) throws IllegalStateException {
        if (value instanceof String) {
            String ref = (String) value;
            switch (ref) {
            case "red":
            case "RED":
            case "FF0000":
            case "F00":
                return Color.RED;
            case "green":
            case "GREEN":
            case "00FF00":
            case "0F0":
                return Color.GREEN;
            case "blue":
            case "BLUE":
            case "0000FF":
            case "00F":
                return Color.BLUE;
            }
        }
        return null;
    }

    @Override
    public Serializable getReference(Color color) throws IllegalStateException, IllegalArgumentException {
        return color != null ? color.name() : null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        return invalidValue + " is not a correct value";
    }

}
