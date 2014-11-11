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
 * $Id: TransformService.java 30390 2008-02-21 01:42:54Z tdelprat $
 */

package org.nuxeo.ecm.platform.transform.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Registry;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.extensions.PluginExtensionPointHandler;
import org.nuxeo.ecm.platform.transform.service.extensions.TransformerExtensionPointHandler;
import org.nuxeo.ecm.platform.transform.transformer.TransformerImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * TransformServiceCommon service.
 * <p>
 * It handles a registry of plugins and transformers.
 * <p>
 * This is the component to request to perform transformations. See API.
 * <p>
 * You may take advantage of extension points defined in TransformServiceCommon
 * to register your own plugins and transformers
 *
 * @see org.nuxeo.ecm.platform.transform.interfaces.Plugin
 *
 * @see org.nuxeo.ecm.platform.transform.interfaces.Transformer
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TransformService extends DefaultComponent implements
        TransformServiceCommon {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.transform.service.TransformService");

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TransformService.class);

    private final PluginExtensionPointHandler pluginExtensionHandler;

    private final TransformerExtensionPointHandler transformerExtensionHandler;

    private final Registry<Plugin> pluginsRegistry;

    private final Registry<Transformer> transformersRegistry;

    public TransformService() {
        pluginExtensionHandler = new PluginExtensionPointHandler();
        transformerExtensionHandler = new TransformerExtensionPointHandler();

        pluginsRegistry = new Registry<Plugin>(TransformService.class.getName());
        transformersRegistry = new Registry<Transformer>(
                TransformService.class.getName());
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        // TODO: put initialization here! not in ctor
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // TODO shutdown registries here
    }

    @Override
    public void registerExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("plugins")) {
            pluginExtensionHandler.registerExtension(extension);
        } else if (extension.getExtensionPoint().equals("transformers")) {
            transformerExtensionHandler.registerExtension(extension);
        } else {
            log.error("Unknown contributions... can't register !");
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("plugins")) {
            pluginExtensionHandler.unregisterExtension(extension);
        } else if (extension.getExtensionPoint().equals("transformers")) {
            transformerExtensionHandler.unregisterExtension(extension);
        } else {
            log.error("Unknown contributions... can't unregister!");
        }
    }

    public Plugin getPluginByName(String name) {
        return pluginsRegistry.getObjectByName(name);
    }

    public Plugin getPluginByMimeTypes(String sourceMT, String destinationMT) {
        for (String pluginName : pluginsRegistry.getKeys()) {
            Plugin plugin = pluginsRegistry.getObjectByName(pluginName);

            if (plugin.getSourceMimeTypes().contains(sourceMT)) {
                if (plugin.getDestinationMimeType().contains(destinationMT)) {
                    return plugin;
                }
            }
        }
        log.debug(
                "Can not find registred plugin for transforming " + sourceMT + " to " + destinationMT);
        return null;
    }

    public List<Plugin> getPluginByDestinationMimeTypes(String destinationMT) {
        List<Plugin> result = new ArrayList<Plugin>();

        for (String pluginName : pluginsRegistry.getKeys()) {
            Plugin plugin = pluginsRegistry.getObjectByName(pluginName);

            if (plugin.getDestinationMimeType().contains(destinationMT)) {
                result.add(plugin);
            }
        }
        return result;
    }

    public Transformer getTransformerByName(String name) {
        Transformer transformer = transformersRegistry.getObjectByName(name);
        if (transformer == null) {
            // try a create a dummy transformer that simply wraps a single Plugin
            Plugin plugin = getPluginByName(name);
            if (plugin != null) {
                transformer = new TransformerImpl();
                transformer.setName(name);
                List<String> chain = new ArrayList<String>();
                chain.add(name);
                transformer.setPluginChains(chain);
                transformersRegistry.register(name, transformer);
            }
        }
        return transformer;
    }

    public void registerPlugin(String name, Plugin plugin) {
        // : XXX : merge plugins options so that we can override from third
        // party
        // code.
        pluginsRegistry.register(name, plugin);
        log.debug("Registering plugin: " + name);
    }

    public void registerTransformer(String name, Transformer transformer) {
        // : XXX : merge transformers options so that we can override from third
        // party code.
        transformersRegistry.register(name, transformer);
        log.debug("Registering transformer: " + name);
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) throws TransformException {

        List<TransformDocument> results = new ArrayList<TransformDocument>();

        Transformer tr = transformersRegistry.getObjectByName(transformerName);

        if (tr == null) {
            log.warn("Transformer with name=" + transformerName
                    + " doesn't exist!");
            return results;
        } else {
            log.debug("Requesting transformer with name=" + transformerName);
            return tr.transform(options, sources);
        }
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            Blob... blobs) throws TransformException {

        TransformDocument[] tds = new TransformDocument[blobs.length];
        for (int i=0; i < blobs.length; i++) {
            tds[i] = new TransformDocumentImpl(blobs[i]);
        }

        return transform(transformerName, options,tds);
    }

    public void unregisterPlugin(String name) {
        pluginsRegistry.unregister(name);
        log.debug("Unregistering plugin: " + name);
    }

    public void unregisterTransformer(String name) {
        transformersRegistry.unregister(name);
        log.debug("Unregistering transformer: " + name);
    }

    public boolean isMimetypeSupportedByPlugin(String pluginName,
            String mimetype) {
        Plugin plugin = getPluginByName(pluginName);
        List<String> sourceMimetype = plugin.getSourceMimeTypes();
        return sourceMimetype.contains(mimetype);
    }

}
