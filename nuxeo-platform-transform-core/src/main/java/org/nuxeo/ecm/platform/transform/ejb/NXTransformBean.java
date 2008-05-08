/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformBean.java 30390 2008-02-21 01:42:54Z tdelprat $
 */

package org.nuxeo.ecm.platform.transform.ejb;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.transform.NXTransform;
import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.interfaces.ejb.local.NXTransformLocal;
import org.nuxeo.ecm.platform.transform.interfaces.ejb.remote.NXTransformRemote;
import org.nuxeo.ecm.platform.transform.service.TransformService;

/**
 * TransformServiceCommon EJB component.
 * <p>
 * EJB facade for the TransformComponent.
 *
 * @see org.nuxeo.ecm.platform.transform.service.TransformService
 * @see org.nuxeo.ecm.platform.transform.interfaces.Plugin
 * @see org.nuxeo.ecm.platform.transform.interfaces.Transformer
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(NXTransformLocal.class)
@Remote(NXTransformRemote.class)
public class NXTransformBean implements TransformServiceCommon {

    private static final Log log = LogFactory.getLog(NXTransformBean.class);

    private static final long serialVersionUID = 1L;

    protected TransformService service;


    private TransformServiceCommon getService() {
        if (service == null) {
            service = NXTransform.getTransformService();
        }
        return service;
    }

    public Plugin getPluginByName(String name) {
        if (getService() != null) {
            return getService().getPluginByName(name);
        }
        return null;
    }

    public Plugin getPluginByMimeTypes(String sourceMT, String destinationMT) {
        if (getService() != null) {
            return getService().getPluginByMimeTypes(sourceMT, destinationMT);
        }
        return null;
    }

    public Transformer getTransformerByName(String name) {
        if (getService() != null) {
            return getService().getTransformerByName(name);
        }
        return null;
    }

    public void registerPlugin(String name, Plugin plugin) {
        if (getService() != null) {
            getService().registerPlugin(name, plugin);
        }
    }

    public void registerTransformer(String name, Transformer transformer) {
        if (getService() != null) {
            getService().registerTransformer(name, transformer);
        }
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) throws TransformException {
        if (getService() != null) {
            return getService().transform(transformerName, options, sources);
        }
        return null;
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options, Blob... sources)
            throws TransformException {
        if (getService() != null) {
            return getService().transform(transformerName, options, sources);
        }
        return null;
    }

    public void unregisterPlugin(String name) {
        if (getService() != null) {
            getService().unregisterPlugin(name);
        }
    }

    public void unregisterTransformer(String name) {
        if (getService() != null) {
            getService().unregisterTransformer(name);
        }
    }

    public boolean isMimetypeSupportedByPlugin(String pluginName,
            String mimetype) {
        if (getService() != null) {
            return getService().isMimetypeSupportedByPlugin(pluginName,
                    mimetype);
        }
        return false;
    }

    public List<Plugin> getPluginByDestinationMimeTypes(String destinationMT) {
        if (getService() != null) {
            return getService().getPluginByDestinationMimeTypes(destinationMT);
        }
        return null;
    }

}
