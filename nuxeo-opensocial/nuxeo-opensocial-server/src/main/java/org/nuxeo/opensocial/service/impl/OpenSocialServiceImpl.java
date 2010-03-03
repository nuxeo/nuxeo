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

}
