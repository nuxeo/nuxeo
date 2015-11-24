/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.key;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialResolver;

import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * A manager for {@link Credential}s.
 *
 * @since 5.9.6
 */
public interface KeyManager extends CredentialResolver {

    Credential getCredential(String keyName);

    Set<String> getAvailableCredentials();

    X509Certificate getCertificate(String alias);

    Credential getSigningCredential();

    Credential getEncryptionCredential();

    Credential getTlsCredential();
}
