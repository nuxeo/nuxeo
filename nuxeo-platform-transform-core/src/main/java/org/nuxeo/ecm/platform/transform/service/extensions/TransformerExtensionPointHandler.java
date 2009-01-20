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
 * $Id: TransformerExtensionPointHandler.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.service.extensions;

import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.runtime.model.Extension;

/**
 * Extension point handler for transformers.
 *
 * @author janguenot
 *
 */
public class TransformerExtensionPointHandler extends
        NXTransformExtensionPointHandler {

    public static void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            TransformerExtension transformerExtension = (TransformerExtension) contrib;

            try {
                unregisterOne(transformerExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            TransformerExtension transformerExtension = (TransformerExtension) contrib;

            try {
                registerOne(transformerExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static void registerOne(TransformerExtension transformerExtension,
            Extension extension) throws Exception {

        String name = transformerExtension.getName();
        String className = transformerExtension.getClassName();
        TransformerExtensionPluginsConfiguration pluginsChain = transformerExtension.getPluginsChain();

        try {
            Transformer transformer = (Transformer) extension.getContext()
                    .loadClass(className).newInstance();
            transformer.setName(name);
            log.debug("Registering plugin chain: " + pluginsChain.getPluginsChain());
            transformer.setPluginChains(pluginsChain.getPluginsChain());
            transformer.setDefaultOptions(pluginsChain.getDefaultPluginOptions());

            TransformServiceCommon transformService = getNXTransform();
            if (transformService != null) {
                transformService.registerTransformer(name, transformer);
            } else {
                log.error("No TransformServiceCommon service found impossible to register transformer");
            }
        // XXX: dubious code
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private static void unregisterOne(TransformerExtension transformerExtension,
            Extension extension) {
        String name = transformerExtension.getName();
        getNXTransform().unregisterTransformer(name);
    }

}
