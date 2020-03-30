/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.key;

import java.security.cert.X509Certificate;
import java.util.Set;

import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;

/**
 * A manager for {@link Credential}s.
 *
 * @since 6.0
 */
public interface KeyManager extends CredentialResolver {

    Credential getCredential(String keyName);

    Set<String> getAvailableCredentials();

    X509Certificate getCertificate(String alias);

    Credential getSigningCredential();

    Credential getEncryptionCredential();

    Credential getTlsCredential();
}
