package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * ValuePair
 *
 * @author Guillaume Cusnieux
 */
public class ValuePair implements IsSerializable {

  private String value;
  private String displayValue;

  public ValuePair() {
  }

  public ValuePair(String value, String displayValue) {
    this.value = value;
    this.displayValue = displayValue;
  }

  public String getValue() {
    return value;
  }

  public String getDisplayValue() {
    return displayValue;
  }
}
