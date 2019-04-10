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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.core.pki;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Provides configuration information for root certificate generation services.
 * <p>
 * As the root keystore needs to be configurable by the system administrator, this configuration object allows the
 * administrator to store the root keystore location and access information as XML elements. This information is used by
 * the certificate authority services for signing user certificates and for exposing the root certificate object to the
 * user interface.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */

@XObject("configuration")
public class RootDescriptor {

    @XNode("rootKeystoreFilePath")
    protected String rootKeystoreFilePath;

    @XNode("rootKeystorePassword")
    protected String rootKeystorePassword;

    @XNode("rootCertificateAlias")
    protected String rootCertificateAlias;

    @XNode("rootKeyAlias")
    protected String rootKeyAlias;

    @XNode("rootKeyPassword")
    protected String rootKeyPassword;

    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

    public void setRootKeyAlias(String rootKeyAlias) {
        this.rootKeyAlias = rootKeyAlias;
    }

    public String getRootKeyPassword() {
        return rootKeyPassword;
    }

    public void setRootKeyPassword(String rootKeyPassword) {
        this.rootKeyPassword = rootKeyPassword;
    }

    public String getRootKeystorePassword() {
        return rootKeystorePassword;
    }

    public void setRootKeystorePassword(String rootKeystorePassword) {
        this.rootKeystorePassword = rootKeystorePassword;
    }

    public String getRootCertificateAlias() {
        return rootCertificateAlias;
    }

    public void setRootCertificateAlias(String rootCertificateAlias) {
        this.rootCertificateAlias = rootCertificateAlias;
    }

    public String getRootKeystoreFilePath() {
        return rootKeystoreFilePath;
    }

    public void setRootKeystoreFilePath(String rootKeystoreFilePath) {
        this.rootKeystoreFilePath = rootKeystoreFilePath;
    }

    private boolean remove;

    @XNode("removeExtension")
    protected void setRemoveExtension(boolean remove) {
        this.remove = remove;
    }

    public boolean getRemoveExtension() {
        return remove;
    }
}
