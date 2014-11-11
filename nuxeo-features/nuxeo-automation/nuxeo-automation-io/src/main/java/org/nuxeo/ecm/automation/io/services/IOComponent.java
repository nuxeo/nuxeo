/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services;

import org.nuxeo.ecm.automation.io.services.codec.CodecDescriptor;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 *
 * @since 5.7.3
 */
public class IOComponent extends DefaultComponent {


    protected static final String XP_CODECS = "codecs";


    private JsonFactoryManager jsonFactoryManager;

    private ObjectCodecService codecs;

    @Override
    public void activate(ComponentContext context) throws Exception {
        jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        codecs = new ObjectCodecService(jsonFactoryManager.getJsonFactory());
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_CODECS.equals(extensionPoint)) {
            CodecDescriptor codec = (CodecDescriptor) contribution;
            codecs.addCodec(codec.newInstance());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_CODECS.equals(extensionPoint)) {
            CodecDescriptor codec = (CodecDescriptor) contribution;
            codecs.removeCodec(codec.newInstance().getJavaType());
        }
    }


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (ObjectCodecService.class.isAssignableFrom(adapter)) {
            return adapter.cast(codecs);
        } else  if(JsonFactoryManager.class.isAssignableFrom(adapter)) {
            return adapter.cast(jsonFactoryManager);
        }
        return null;
    }


    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        codecs.postInit();
    }

}
