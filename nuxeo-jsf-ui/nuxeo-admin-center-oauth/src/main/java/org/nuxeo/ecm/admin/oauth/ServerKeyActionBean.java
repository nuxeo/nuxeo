/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.runtime.api.Framework;

@Name("oauthServerKeyActions")
@Scope(ScopeType.EVENT)
public class ServerKeyActionBean {

    public String getPublicCertificate() {
        OAuthServerKeyManager skm = Framework.getService(OAuthServerKeyManager.class);
        return skm.getPublicKeyCertificate();
    }

    public String getKeyName() {
        OAuthServerKeyManager skm = Framework.getService(OAuthServerKeyManager.class);
        return skm.getKeyName();
    }

}
