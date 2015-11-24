/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.gadgets.GadgetSpecFactory;

import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.servlet.GuiceContextListener;
import org.nuxeo.opensocial.shindig.crypto.OAuthServiceDescriptor;
import org.nuxeo.opensocial.shindig.crypto.OpenSocialDescriptor;
import org.nuxeo.opensocial.shindig.crypto.PortalConfig;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.inject.Injector;

public class OpenSocialServiceImpl extends DefaultComponent implements
        OpenSocialService {

    private static final Log log = LogFactory.getLog(OpenSocialServiceImpl.class);

    public static final String ID = "org.nuxeo.opensocial.service.impl.OpenSocialServiceImpl";

    public static final ComponentName NAME = new ComponentName(ID);

    private static final Log LOG = LogFactory.getLog(OpenSocialService.class);

    private static final String XP_OPENSOCIAL = "openSocialConfig";

    private static Injector injector;

    protected File signingStateKeyFile;

    protected OpenSocialDescriptor os;

    private final Map<String, String> keys = new HashMap<>();

    protected String signingStateKeyBytes;

    public Injector getInjector() {
        return injector;
    }

    @Override
    public GadgetSpecFactory getGadgetSpecFactory() {
        return injector.getInstance(GadgetSpecFactory.class);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_OPENSOCIAL.equals(extensionPoint)) {
            os = (OpenSocialDescriptor) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(this.getClass())) {
            return (T) this;
        }

        // Try the inject to find the class
        try {
            return getInjector().getInstance(adapter);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        LOG.info("Activate component OpenSocial service");
        if (injector == null) {
            injector = GuiceContextListener.guiceInjector;
        }
    }

    @Override
    public void deactivate(ComponentContext arg0) {
        LOG.info("DeActivate component OpenSocial service");
    }

    public Object getInstance(Class<?> klass) {
        if (getInjector() != null) {
            return getInjector().getInstance(klass);
        } else {
            return null;
        }
    }

    @Override
    public void setInjector(Injector injector) {
        OpenSocialServiceImpl.injector = injector;
    }

    public String getKeyForContainer(String defaultContainer) {
        return keys.get(defaultContainer);
    }

    @Override
    public void setupOpenSocial() throws Exception {
        if (os == null) {
            log.warn("OpenSocial does not have any configuration contribution ... setup canceled");
            return;
        }
        // state key
        if (StringUtils.isBlank(os.getSigningKey())) {
            byte[] b64 = Base64.encodeBase64(Crypto.getRandomBytes(BasicBlobCrypter.MASTER_KEY_MIN_LEN));
            os.setSigningKey(new String(b64, "UTF-8"));
        }
        try {
            signingStateKeyFile = createTempFileForAKey(os.getSigningKey());
            // shindig doesn't make it's constants visible to us
            System.setProperty("shindig.signing.state-key",
                    signingStateKeyFile.getPath());
        } catch (IOException e) {
            log.warn("ignoring signing key " + os.getSigningKey()
                    + " because we cannot write temp file!", e);
        }

        // callback URL
        if (!StringUtils.isBlank(os.getCallbackUrl())) {
            // shindig doesn't make it's constants visible to us
            System.setProperty("shindig.signing.global-callback-url",
                    os.getCallbackUrl());
        } else {
            throw new Exception(
                    "Unable to start because the global callback url"
                            + " is not set.  See default-opensocial-config.xml");
        }

    }

    // if you are worried about the security of this, you probably should be
    // however, this is required (as far as I can tell)
    // by the design of the shindig system--which does make some
    // assumptions that the container is reasonably secure
    protected File createTempFileForAKey(String keyValue) throws IOException {
        File f = File.createTempFile("nxkey", ".txt");
        FileWriter writer = new FileWriter(f);
        writer.append(keyValue);
        writer.flush();
        writer.close();
        return f;
    }

    @Override
    public File getSigningStateKeyFile() {
        return signingStateKeyFile;
    }

    @Override
    public PortalConfig[] getPortalConfig() {
        return os.getPortalConfig();
    }

    @Override
    public OAuthServiceDescriptor[] getOAuthServices() {
        return os.getOAuthServices();
    }

    @Override
    public String getOAuthCallbackUrl() {
        return os.getCallbackUrl();
    }

    @Override
    public String[] getTrustedHosts() {
        return os.getTrustedHosts();
    }

    @Override
    public boolean isTrustedHost(String host) {
        return os.isTrustedHost(host);
    }

    @Override
    public byte[] getSigningStateKeyBytes() {
        try {
            if (signingStateKeyBytes == null) {
                signingStateKeyBytes = IOUtils.toString(new FileReader(
                        getSigningStateKeyFile()));
            }
            return signingStateKeyBytes.getBytes();
        } catch (FileNotFoundException e) {
            log.error("Unable to find the signing key file! "
                    + "Check default-opensocial-contrib.xml!", e);
            return null;
        } catch (IOException e) {
            log.error("Unable to read the signing key file! "
                    + "Check default-opensocial-contrib.xml!", e);
            return null;
        }
    }

    @Override
    public boolean propagateJSESSIONIDToTrustedHosts() {
        return os.propagateJSESSIONIDToTrustedHosts();
    }

}
