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

package org.nuxeo.opensocial.shindig.crypto;

import java.util.Map;

import org.apache.shindig.auth.BasicSecurityTokenDecoder;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.ContainerConfig;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;



public class NXSecurityTokenDecoder implements SecurityTokenDecoder {

    private static final String SECURITY_TOKEN_TYPE = "gadgets.securityTokenType";


      private final SecurityTokenDecoder decoder;

      @Inject
      public NXSecurityTokenDecoder(ContainerConfig config) throws Exception {
        String tokenType = config.get(ContainerConfig.DEFAULT_CONTAINER, SECURITY_TOKEN_TYPE);
        if ("insecure".equals(tokenType)) {
          decoder = new BasicSecurityTokenDecoder();
        } else if ("secure".equals(tokenType)) {
            OpenSocialService os = Framework.getService(OpenSocialService.class);
            String key = os.getKeyForContainer(ContainerConfig.DEFAULT_CONTAINER);
            decoder = new BlobCrypterSecurityTokenDecoder(config, key);
        } else {
          throw new RuntimeException("Unknown security token type specified in " +
              ContainerConfig.DEFAULT_CONTAINER + " container configuration. " +
              SECURITY_TOKEN_TYPE + ": " + tokenType);
        }
      }

      public SecurityToken createToken(Map<String, String> tokenParameters)
          throws SecurityTokenException {
        return decoder.createToken(tokenParameters);
      }


}
