/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PrefetchInfo implements Serializable {

    private static final long serialVersionUID = -6495547095819614741L;

    private static final Log log = LogFactory.getLog(PrefetchInfo.class);

    private final String expr;
    private transient Field[] fields;
    private transient Schema[] schemas;

    public PrefetchInfo(String expr) {
        assert expr != null;
        this.expr = expr;
    }

    public Schema[] getSchemas() {
        if (schemas == null && expr != null) {
            parseExpression();
        }
        return schemas;
    }

    public Field[] getFields() {
        if (fields == null && expr != null) {
            parseExpression();
        }
        return fields;
    }

    private void parseExpression() {
        SchemaManager typeMgr = Framework.getLocalService(SchemaManager.class);
        List<Field> fields = new ArrayList<Field>();
        List<Schema> schemas = new ArrayList<Schema>();
        StringTokenizer st = new StringTokenizer(expr, " \t\n\r,");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            int len = tok.length();
            if (len > 0) {
                // try schema_prefix:field_name format
                int p = tok.indexOf(':');
                if (p > -1) {
                    Field field = typeMgr.getField(tok);
                    if (field != null) {
                        fields.add(field);
                    } else {
                        log.error("Field  '" + tok + "' not found for prefetching");
                    }
                    continue;
                }
                // try schema_name.field_name format
                p = tok.indexOf('.');
                if (p > -1) {
                    String schemaName = tok.substring(0, p);
                    String fieldName = tok.substring(p + 1);
                    Schema schema = typeMgr.getSchema(schemaName);
                    if (schema != null) {
                        Field field = schema.getField(fieldName);
                        if (field != null) {
                            fields.add(field);
                            continue;
                        }
                    }
                    log.error("Field '" + tok + "' could not be resolved for prefetching");
                    continue;
                }
                // should be a schema name
                Schema schema = typeMgr.getSchema(tok);
                if (schema != null) {
                    schemas.add(schema);
                } else {
                    log.error("Schema '" + tok + "' not found for prefetching");
                }
            }
        }
        this.fields = fields.toArray(new Field[fields.size()]);
        this.schemas = schemas.toArray(new Schema[schemas.size()]);
    }

}
