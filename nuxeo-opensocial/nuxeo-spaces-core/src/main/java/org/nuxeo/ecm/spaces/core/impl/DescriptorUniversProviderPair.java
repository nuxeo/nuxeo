package org.nuxeo.ecm.spaces.core.impl;

import org.nuxeo.ecm.spaces.core.contribs.api.UniversProvider;

public class DescriptorUniversProviderPair extends DescriptorProviderPair<UniversContribDescriptor,UniversProvider>{


  public DescriptorUniversProviderPair(
      UniversProvider provider,
      UniversContribDescriptor descriptor) {
    super(descriptor,provider);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof DescriptorUniversProviderPair){
      return getDescriptor().getName().equals(((DescriptorUniversProviderPair)obj).getDescriptor().getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
