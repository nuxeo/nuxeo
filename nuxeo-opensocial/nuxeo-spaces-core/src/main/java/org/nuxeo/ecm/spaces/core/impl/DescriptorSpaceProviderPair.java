package org.nuxeo.ecm.spaces.core.impl;

import org.nuxeo.ecm.spaces.core.contribs.api.SpaceProvider;

public class DescriptorSpaceProviderPair extends
    DescriptorProviderPair<SpaceContribDescriptor, SpaceProvider> {

  DescriptorSpaceProviderPair(SpaceContribDescriptor descriptor,SpaceProvider provider) {
    super( descriptor,provider);
  }

  @Override
  public boolean equals(Object obj) {
      if(obj instanceof DescriptorSpaceProviderPair){
      return getDescriptor().getName().equals(((DescriptorSpaceProviderPair)obj).getDescriptor().getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
