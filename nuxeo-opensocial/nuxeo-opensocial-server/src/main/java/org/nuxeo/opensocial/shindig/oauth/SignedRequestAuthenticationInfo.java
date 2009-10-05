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

package org.nuxeo.opensocial.shindig.oauth;

import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;

public class SignedRequestAuthenticationInfo implements
    RequestAuthenticationInfo {

  private static final String SIGN_OWNER_PARAM = "signOwner";
  private static final String SIGN_VIEWER_PARAM = "signViewer";

  private Uri url;

  private Map<String,String> attributes;

  public SignedRequestAuthenticationInfo(String ownerId, String viewerId, Uri url) {
    this.url = url;

    attributes = new HashMap<String,String>();
    attributes.put(SIGN_OWNER_PARAM, ownerId);
    attributes.put(SIGN_VIEWER_PARAM, viewerId);
  }

  public Map<String, String> getAttributes() {
    // TODO Auto-generated method stub
    return attributes;
  }

  public AuthType getAuthType() {
    // TODO Auto-generated method stub
    return AuthType.SIGNED;
  }

  public Uri getHref() {
    // TODO Auto-generated method stub
    return url;
  }

  public boolean isSignOwner() {
    return true;
  }

  public boolean isSignViewer() {

    return true;
  }

}
