/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

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
