/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.compat;

import java.util.List;

import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.extensions.TransformerExtension;

public class TransformerCompatConverterDescriptor extends ConverterDescriptor {

    private static final long serialVersionUID = 1L;

    protected Class<Transformer> transformerClass;

    protected TransformerExtension transformerDescriptor;

    public TransformerCompatConverterDescriptor(TransformerExtension transformerExtension) {
        List<String> pluginChain = transformerExtension.getPluginsChain().getPluginsChain();
        subConverters = pluginChain;
        steps = null;
        converterName = transformerExtension.getName();
        sourceMimeTypes = ConversionServiceImpl.getConverterDesciptor(pluginChain.get(0)).getSourceMimeTypes();
        destinationMimeType = ConversionServiceImpl.getConverterDesciptor(pluginChain.get(pluginChain.size() - 1)).getDestinationMimeType();
    }

}
