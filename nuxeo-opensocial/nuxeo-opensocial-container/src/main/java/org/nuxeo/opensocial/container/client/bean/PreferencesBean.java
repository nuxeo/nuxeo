package org.nuxeo.opensocial.container.client.bean;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class PreferencesBean implements IsSerializable {

  private String dataType;
  private String defaultValue;
  private String displayName;
  private List<ValuePair> enumValues;
  private String name;
  private String value;

  /**
   * Default construcor (Specification of Gwt)
   */
  public PreferencesBean() {
  }

  public PreferencesBean(String dataType, String defaultValue,
      String displayName, List<ValuePair> enumValues, String name, String value) {
    this.dataType = dataType;
    this.defaultValue = defaultValue;
    this.displayName = displayName;
    this.enumValues = enumValues;
    this.name = name;
    this.value = value;
  }

  public String getDataType() {
    return this.dataType;
  }

  public String getDefaultValue() {
    return this.defaultValue;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public List<ValuePair> getEnumValues() {
    return this.enumValues;
  }

  public String getName() {
    return this.name;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
