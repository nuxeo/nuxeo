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

package org.nuxeo.opensocial.service.impl;

import java.net.ProxySelector;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.servlet.GuiceContextListener;
import org.nuxeo.opensocial.shindig.crypto.KeyDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.inject.Injector;

public class OpenSocialServiceImpl extends DefaultComponent implements
        OpenSocialService {
    public static final String ID = "org.nuxeo.opensocial.service.impl.OpenSocialServiceImpl";

    public static final ComponentName NAME = new ComponentName(ID);

    private static final Log LOG = LogFactory.getLog(OpenSocialService.class);

    private static final String XP_CRYPTO = "cryptoConfig";

    private static Injector injector;

    private final Map<String, String> keys = new HashMap<String, String>();

    private static final String configStr =

    "{"
            + "\"http://localhost:8080/nuxeo/site/gadgets/confluencefeed/confluencefeed.xml\" : {"
            + "\"\" : {"
            + "\"consumer_key\" : \"nuxeo-opensocial\","
            + "\"consumer_secret\" : \"-----BEGIN PRIVATE KEY----- MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAM7wu+HCQuBfVyPM TgA9SZh8jcqY5ZF51N2GuwWcLLfhB7/wdj3iE8d564raH52FU2onnoOqry6u/A1t DzKy1riK7g6p3pubP0x9oAaPnkDCVUAPimAvGuZSWBSr3ryDP5GHgI/VYAOiXASM TISq5qxpmat54trYQJFN3iSh0spZAgMBAAECgYBzg8/s8opwQugalIYJ/iwh0Y04 xWaIcVCQpA+rzwTrU9MGoozueE+ALx97b8zsGit4+0qxxsppLcaHHBS6wTe35ML8 OggORPf0xEQAZpYRZeMX91sDNNVVooGTAOh5htH5E9eRqbvlsALO8/Ket8+virvk o6wcGo05Z9yjyT8ssQJBAO3izTwkXC+raqgV4TP7jchesgKTDvScBiZEtVqFston 9U5A5M3eEbHOBgMVKji3BPyGCTFftC2LZl7VfzQWqi0CQQDesrLc1FONfMNpyEgX QcQSg9Au/xhLq+AKUupozRCin25VXH0Jqn6KMdANKZdLt2wuDTUUL0Nd+06Le6Lj pdhdAkEAx5ADwpdyKp9wG1A3m8dFWzlttlEuM7CMTCBJz4Xn07G/zYUNLVNFntcK Hh3sTKXk7f93yM7TtX2DRL1wN/9nhQJBAMnOjDF7o7+aqQbqPRH+Qe05T+XWuzCP r3YLj2qrMgD8kyJ9rr2cqBEZdN0IrJcrv7e3tjr1XYoEGzhhMMo01u0CQQC7Kky2 +IQgLJ2EwBNzqAgH9UglOwwPKp4sYGnr63Po660N8BvJKBPErFx8fHE6isxyrAAp CtChzksnyjXXLZUO -----END PRIVATE KEY-----\","
            + "\"key_type\" : \"RSA_PRIVATE\"" + "}" + "}" + "}";

    public Injector getInjector() {
        return injector;
    }

    public GadgetSpecFactory getGadgetSpecFactory() {
        return injector.getInstance(GadgetSpecFactory.class);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_CRYPTO.equals(extensionPoint)) {
            KeyDescriptor kd = (KeyDescriptor) contribution;
            keys.put(kd.getContainer(), kd.getKey());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_CRYPTO.equals(extensionPoint)) {
            KeyDescriptor kd = (KeyDescriptor) contribution;
            if (keys.containsKey(kd.getContainer())) {
                keys.remove(kd.getContainer());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(this.getClass())) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
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
        if (getInjector() != null)
            return getInjector().getInstance(klass);
        else
            return null;
    }

    public void setInjector(Injector injector) {
        OpenSocialServiceImpl.injector = injector;
    }

    public String getKeyForContainer(String defaultContainer) {
        return keys.get(defaultContainer);
    }

    public ProxySelector getProxySelector() {
        return new SimpleProxySelector();
    }

    public String getOAuthServiceConfig() {
        return (configStr);
    }

}
