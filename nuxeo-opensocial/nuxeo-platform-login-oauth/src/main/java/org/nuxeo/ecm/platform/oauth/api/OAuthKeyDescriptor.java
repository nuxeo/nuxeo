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

package org.nuxeo.ecm.platform.oauth.api;

import net.oauth.OAuthConsumer;
import net.oauth.signature.RSA_SHA1;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("keys")
public class OAuthKeyDescriptor {

  @XNode("@consumer")
  public String consumer;

  @XNode("public-key")
  public String publicKey;

  @XNode("private-key")
  public String privateKey;

  @XNode("certificate")
  public String certificate;

  public void setKeyForConsumer(OAuthConsumer consumer) {
    if("".equals(certificate)) {
      consumer.setProperty(RSA_SHA1.PUBLIC_KEY,publicKey);
    } else {
      consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, certificate);
    }
  }
}
