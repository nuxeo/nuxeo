package org.nuxeo.ecm.spaces.core.impl;

import org.nuxeo.ecm.spaces.core.contribs.api.GadgetProvider;

public class DescriptorGadgetProviderPair extends
    DescriptorProviderPair<GadgetContribDescriptor, GadgetProvider> {

  DescriptorGadgetProviderPair(GadgetProvider provider,
      GadgetContribDescriptor descriptor) {
    super( descriptor,provider);
  }
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof DescriptorGadgetProviderPair){
      return getDescriptor().getName().equals(((DescriptorGadgetProviderPair)obj).getDescriptor().getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
