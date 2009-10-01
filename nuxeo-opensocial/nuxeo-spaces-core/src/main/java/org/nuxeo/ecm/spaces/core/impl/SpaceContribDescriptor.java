package org.nuxeo.ecm.spaces.core.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;


@XObject("spaceContrib")
public class SpaceContribDescriptor  {

  @XNode("@name")
  private String name;

  @XNode("@remove")
  private boolean remove;

  @XNode("className")
  private String className;

  @XNode("order")
  private String order;

  @XNode("restrictToUniverse")
  private String pattern;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isRemove() {
    return remove;
  }

  public void setRemove(boolean remove) {
    this.remove = remove;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

}
