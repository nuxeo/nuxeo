package org.nuxeo.ecm.spaces.core.impl;


public class DescriptorProviderPair<D, P> {


  public DescriptorProviderPair(D descriptor, P provider) {
    super();
    this.descriptor = descriptor;
    this.provider = provider;
  }

  public P getProvider() {
    return provider;
  }

  public D getDescriptor() {
    return descriptor;
  }

  private final P provider;
  private final D descriptor;


}
