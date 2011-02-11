package org.nuxeo.opensocial.container.shared.webcontent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;

public class UserPref implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private String displayName;

    private String defaultValue;

    private String actualValue;

    private DataType dataType;

    private Map<String, String> enumValues;

    @SuppressWarnings("unused")
    private UserPref() {
    }

    public UserPref(String name, DataType dataType) {
        setEnumValues(new HashMap<String, String>());
        setName(name);
        setDataType(dataType);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setEnumValues(Map<String, String> enumValues) {
        this.enumValues = enumValues;
    }

    public Map<String, String> getEnumValues() {
        return enumValues;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public String getActualValue() {
        return actualValue;
    }
}
