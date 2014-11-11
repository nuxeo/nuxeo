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
 * $Id: TransformServiceCommon.java 21356 2007-06-25 16:41:26Z tdelprat $
 */

package org.nuxeo.ecm.platform.transform.interfaces;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.transform.api.TransformException;

/**
 * TransformServiceCommon service default interface.
 * <p>
 * NXP service dealing with documents transformations.
 * <p>
 * It holds 2 registries:
 * <ul>
 * <li>Transformers registry</li>
 * <li>Plugins registry</li>
 * </ul>
 *
 * @see org.nuxeo.ecm.platform.transform.interfaces.Plugin
 * @see org.nuxeo.ecm.platform.transform.interfaces.Transformer
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface TransformServiceCommon extends Serializable {

    /**
     * Gets registered plugin by name.
     *
     * @param name of the plugin
     * @return plugin instance matching name
     */
    Plugin getPluginByName(String name);

    /**
     * Gets registered plugin for transforming one mimetype to another
     *
     * @param sourceMT sourceMimeType
     * @param destinationMT destinationMimeType
     * @return plugin instance matching
     */
    Plugin getPluginByMimeTypes(String sourceMT, String destinationMT);

    /**
     * get all plugin for a given destination Mime Type
     * @param destinationMT
     * @return the list of matching plugins
     */
    List<Plugin> getPluginByDestinationMimeTypes( String destinationMT);

    /**
     * Gets registered transformer by name.
     *
     * @param name of the transformer
     * @return tranformer instance matching name
     */
    Transformer getTransformerByName(String name);

    /**
     * Registers a plugin by name.
     *
     * @param name name for the plugin
     * @param plugin a plugin instance
     *
     */
    void registerPlugin(String name, Plugin plugin);

    /**
     * Registers a transformer.
     *
     * @param name of the transformer
     * @param transformer instance
     *
     */
    void registerTransformer(String name, Transformer transformer);

    /**
     * Transforms given sources given a transformer name to request.
     *
     * @param transformerName transformer name specifying the transformer we
     *            need to request for this transformation. It should be defined
     *            beforehand.
     *
     * @param options a map holding plugin options. The key of the hashtable is
     *            the name of the plugin used within the chain defined by the
     *            transformer.
     *
     * @param sources list of TransformDocument holding the sources we want to
     *            transmit to the specified transformer.
     *
     * @return list of TransformDocument result of the transformer job.
     * @throws TransformException TODO
     *
     */
    List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options, Blob... sources)
            throws TransformException;

    /**
     * Transforms given sources given a transformer name to request.
     *
     * @param transformerName transformer name specifying the transformer we
     *            need to request for this transformation. It should be defined
     *            beforehand.
     *
     * @param options a map holding plugin options. The key of the hashtable is
     *            the name of the plugin used within the chain defined by the
     *            transformer.
     *
     * @param sources list of TransformDocument holding the sources we want to
     *            transmit to the specified transformer.
     *
     * @return list of TransformDocument result of the transformer job.
     * @throws TransformException TODO
     *
     */
    List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) throws TransformException;

    /**
     * Unregisters a plugin by registration name.
     *
     * @param name name of the plugin to unregister.
     */
    void unregisterPlugin(String name);

    /**
     * Unregisters a transformer by registration name.
     *
     * @param name name of the registered transformer to unregister.
     */
    void unregisterTransformer(String name);

    /**
     * Is the given mimetype an allowed SourceMimeType for the plugin?
     *
     * @param pluginName
     * @param mimetype
     * @return
     */
    boolean isMimetypeSupportedByPlugin(String pluginName, String mimetype);

}
