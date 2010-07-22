/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.webapp;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.webapp.bulkedit.FictiveDataModel;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class FictiveDocumentModel extends
        org.nuxeo.ecm.webapp.bulkedit.FictiveDocumentModel {

    public static DocumentModel createFictiveDocumentModelWith(
            List<String> schemas) {
        FictiveDocumentModel doc = new FictiveDocumentModel();
        for (String schema : schemas) {
            DataModel dataModel = doc.dataModels.get(schema);
            if (dataModel == null) {
                dataModel = new FictiveDataModel(schema);
                doc.dataModels.put(schema, dataModel);
            }
        }
        return doc;
    }

    @Override
    public void setPropertyValue(String xpath, Serializable value)
            throws PropertyException, ClientException {
        Path path = new Path(xpath);
        String segment = path.segment(0);
        String prefix = segment.substring(0, segment.indexOf(":"));

        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        Schema schema = mgr.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = mgr.getSchema(prefix);
            if (schema == null) {
                throw new PropertyNotFoundException(xpath,
                        "Could not find registered schema with prefix: "
                                + prefix);
            }
        }

        String name = segment.substring(segment.indexOf(":") + 1, segment.length());
        dataModels.get(schema.getName()).setData(name, value);
    }

}
