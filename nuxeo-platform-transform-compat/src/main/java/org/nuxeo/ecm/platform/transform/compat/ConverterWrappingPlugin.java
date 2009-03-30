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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

public class ConverterWrappingPlugin implements Converter {

    protected Plugin oldPlugin;

    public ConverterWrappingPlugin(Plugin plugin) {
        oldPlugin = plugin;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        // TODO Auto-generated method stub

        List<TransformDocument> tdocs;
        try {
            tdocs = TransformDocumensFactory.wrap(blobHolder);
        } catch (ClientException e1) {
            throw new ConversionException("Error whild converting TransformDocument to BlobHolder", e1);
        }

        List<TransformDocument> result;

        TransformDocument[] tdocsArray = new TransformDocument[tdocs.size()];

        tdocsArray = tdocs.toArray(tdocsArray);

        try {
            result = oldPlugin.transform(parameters, tdocsArray);
        } catch (Exception e) {
            throw new ConversionException("Error while running compa Tranformer plugin", e);
        }

        return new BlobHolderWrappingTransformDocuments(result);
    }

    public void init(ConverterDescriptor descriptor) {
        Map<String, String> params = descriptor.getParameters();
        Map<String, Serializable> options = new HashMap<String, Serializable>();

        for (String k : params.keySet()) {
            options.put(k, params.get(k));
        }

        oldPlugin.setDefaultOptions(options);
    }

}
