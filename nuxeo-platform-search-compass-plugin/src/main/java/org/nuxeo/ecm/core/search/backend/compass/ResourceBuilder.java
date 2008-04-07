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

package org.nuxeo.ecm.core.search.backend.compass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.compass.core.CompassSession;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.Property.Index;
import org.compass.core.Property.Store;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.engine.naming.StaticPropertyPath;
import org.compass.core.lucene.LuceneProperty;
import org.compass.core.lucene.util.LuceneUtils;
import org.compass.core.mapping.rsem.RawResourcePropertyMapping;
import org.nuxeo.common.utils.Null;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.security.SecurityFiltering;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.FieldConstants;

/**
 * Takes care of building one Compass Resource. DO NOT reuse in another session
 * or for another resource.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class ResourceBuilder {

    private static final Log log = LogFactory.getLog(CompassBackend.class);

    private static final String ID_PROPERTY = "nxdoc_id";

    // TODO others: use a better naming for id properties. They don't need
    // to be different from one nuxeo-handled alias to the other.

    private final CompassSession session;

    private final Resource resource;

    public ResourceBuilder(CompassSession session, String alias, String id) {
        this.session = session;
        resource = session.createResource(alias);

        // Primary ids
        // TODO handle potential conflict in naming
        resource.addProperty(session.createProperty(ID_PROPERTY, id,
                Property.Store.YES, Property.Index.UN_TOKENIZED));
    }

    /**
     * This method creates a property, overriding Compass static configuration
     * by the passed arguments.
     * <p>
     * The goal of applying the right analyzer is not reached yet: everything
     * happens as if the Resource itself overrides whatever mappings are in the
     * property.
     * <p>
     * It relies on Compass internals This method is currently unused, kept for
     * further work.
     *
     * @param name
     * @param value
     * @param analyzer
     * @param store
     * @param index
     * @return
     */
    // TODO apply converter TODO integrate or drop
    @SuppressWarnings("unused")
    private Property buildDynamicalProperty(String name, Object value,
            String analyzer, Store store, Index index) {

        Field f = new Field(name, (String) value,
                LuceneUtils.getFieldStore(store),
                LuceneUtils.getFieldIndex(index));
        LuceneProperty lProp = new DynamicLuceneProperty(f);
        RawResourcePropertyMapping pMapping = new RawResourcePropertyMapping();
        pMapping.setAnalyzer(analyzer);
        // Some magic I don't understand yet.
        // Most seems to be dedicated to the field construction actually
        pMapping.setName(name);
        pMapping.setPath(new StaticPropertyPath(name));
        pMapping.setOverrideByName(true);
        lProp.setPropertyMapping(pMapping);
        return lProp;
    }

    /**
     * Property builder. Called by buildResource
     * <p>
     * For typing concerns: we use primarily the Java type of the incoming
     * objects. The passed type string is then used to determine handling:
     * "keyword" and "text" both apply to String data.
     * <p>
     * In some cases we don't need the type string at all. Note that the
     * resultitem constructor probably will, so it's meaningful to have it in
     * the search service configuration.
     *
     * @param name
     * @param value
     * @param type
     * @param indexed
     * @param stored
     * @param multiple
     * @param sortable
     * @param properties TODO
     * @param properties optional map of properties. Musn't be null
     * @param sortOption TODO
     * @throws IndexingException
     */
    @SuppressWarnings("unchecked")
    protected void addProperty(String name, Object value, String type,
            Boolean indexed, Boolean stored, Boolean multiple,
            boolean sortable, Map<String, Serializable> properties,
            String sortOption) throws IndexingException {

        // Get rid of null case first
        if (value == null || value instanceof Null) {
            addTermProperty(name, Util.NULL_MARKER, indexed, stored);
            return;
        }

        if (indexed == null) {
            indexed = Boolean.TRUE;
        }

        if (stored == null) {
            stored = Boolean.FALSE;
        }

        // Multiple properties
        if (multiple) {
            boolean empty;
            if (value instanceof Object[]) {
                Object[] coll = (Object[]) value;
                empty = coll.length == 0;
                for (Object obj : coll) {
                    addProperty(name, obj, type, indexed, stored, false,
                            sortable, properties, sortOption);
                }
            } else { // still support collections
                Collection<Object> coll = (Collection<Object>) value;
                empty = coll.isEmpty();
                for (Object val : coll) {
                    addProperty(name, val, type, indexed, stored, false,
                            sortable, properties, sortOption);
                }
            }
            if (empty) {
                addTermProperty(name, Util.EMPTY_MARKER, indexed, stored);
            }
            return;
        }

        // :FIXME:
        // Discard blobs for now. Either we will call nxtransform and / or use
        // the native backend transformation features.
        if (value instanceof Blob) {
            return;
        }

        if (type != null) {
            type = type.toLowerCase();
        }

        if (log.isDebugEnabled()) {
            log.debug("addProperty name=" + name + " type=" + type + " value="
                    + value);
        }

        if (BuiltinDocumentFields.FIELD_FULLTEXT.equals(name)) {
            name = Util.COMPASS_FULLTEXT;
        }

        // :FIXME: JA : sucks should be done differently !
        String sValue = null;
        if (value instanceof String) {
            value = Util.escapeSpecialMarkers((String) value);
            sValue = (String) value;
        } else if (value instanceof GregorianCalendar) {
            sValue = String.valueOf(((GregorianCalendar) value).getTimeInMillis());
        } else if (value instanceof Date) {
            sValue = String.valueOf(((Date) value).getTime());
        } else if (value instanceof Integer || value instanceof Long
                || value instanceof Boolean || value instanceof Double) {
            sValue = String.valueOf(value);
        }

        // TODO really weak on Property.Index
        Index pIndexed = Property.Index.NO;
        if (indexed) {
            pIndexed = type.equals("keyword") || type.equals("builtin") ? Property.Index.UN_TOKENIZED
                    : Property.Index.TOKENIZED;
        }

        // Sortable configuration.
        if (sortable && sValue != null) {
            final String sname = name + Util.SORTABLE_FIELD_SUFFIX;
            String sortValue;
            if (sortOption != null) {
                sortOption = sortOption.toLowerCase();
            }
            if ("case-insensitive".equals(sortOption)) {
                sortValue = sValue.toLowerCase();
            } else {
                sortValue = sValue;
            }
            // TODO maybe some escaping to be done here
            log.debug("Adding a sort field name=" + sname + " and value="
                    + sortValue);
            addTermProperty(sname, sortValue, true, false);
        }

        try {
            resource.addProperty(name, value);
        } catch (SearchEngineException see) {
            // If Compass doesn't find a PropertyMapping, fall back
            // to lower level creation

            if (value instanceof Path) {
                addPathProperty(name, (Path) value);
                return;
            }

            // Reference builtins. The result item builder will understand that
            if (value instanceof IdRef) {
                addTermProperty(name, "i" + value.toString(), indexed, stored);
                return;
            }

            if (value instanceof PathRef) {
                addTermProperty(name, "p" + value.toString(), indexed, stored);
                return;
            }

            if ("path".equals(type)) {
                if (sValue == null) {
                    log.warn(String.format(
                            "Non-multiple path property %s should be "
                                    + "fed by Path or String instance. Can't index it",
                            name));
                    return;
                }
                addPathProperty(
                        name,
                        sValue,
                        (String) properties.get(FieldConstants.PROPERTY_PATH_SEPARATOR),
                        indexed, stored);
                return;
            }

            if (sValue == null) {
                // novice to serialization. Might be suboptimal
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream o = new ObjectOutputStream(b);
                    o.writeObject(value);
                    o.close();
                } catch (IOException e) {
                    throw new IndexingException(e.getMessage(), e);
                }
                sValue = new sun.misc.BASE64Encoder().encode(b.toByteArray());
            }

            // Note the existence of other Property.Store.* Use Compress on
            // large strings ?
            resource.addProperty(session.createProperty(name, sValue,
                    stored ? Property.Store.YES : Property.Store.NO, pIndexed));

        }
    }

    /**
     * Directly adds a property from a {@link ResolvedData}.
     *
     * @param data the ResolvedData
     * @throws IndexingException
     */
    public void addProperty(ResolvedData data) throws IndexingException {
        addProperty(data.getName(), data);
    }

    /**
     * Directly adds a property from a {@link ResolvedData}, allowing to
     * override the indexing name.
     *
     * @param name name of property to build
     * @param data the resolved data
     * @throws IndexingException
     */
    public void addProperty(String name, ResolvedData data)
            throws IndexingException {
        Map<String, Serializable> props = data.getProperties();
        if (props == null) {
            props = new HashMap<String, Serializable>();
        }
        addProperty(name, data.getValue(), data.getTypeName(),
                data.isIndexed(), data.isStored(), data.isMultiple(),
                data.isSortable(), props, data.getSortOption());
    }

    /**
     * Low level add a property as a single Lucene Term.
     *
     * @param name
     * @param value
     * @param indexed
     * @param stored
     */
    private void addTermProperty(String name, String value, Boolean indexed,
            Boolean stored) {
        log.debug("addTermProperty() name=" + name + " value=" + value);
        resource.addProperty(session.createProperty(name, value,
                stored ? Property.Store.YES : Property.Store.NO,
                indexed ? Property.Index.UN_TOKENIZED
                        : Property.Index.TOKENIZED));
    }

    /**
     * Path indexing and storing.
     * <p>
     * Relies on same principle as was done within nuxeo.lucene: index all
     * subpathes.
     * <p>
     * This could probably optimized by writing a custom Lucene query: less
     * storage, simpler clauses.
     * <p>
     * The whole path is stored.
     *
     * @param name
     * @param path
     */
    public void addPathProperty(String name, Path path) {

        int count = path.segmentCount();
        for (int i = 1; i < count + 1; i++) { // i = 0 : not interesting
            resource.addProperty(session.createProperty(name, path.uptoSegment(
                    i).toString(), i == count ? Property.Store.YES
                    : Property.Store.NO, Property.Index.UN_TOKENIZED));
        }
    }

    /**
     * @param name
     * @param value
     * @param separator The separator, if <code>null</code> defaults to a
     *            slash
     */
    public void addPathProperty(String name, String value, String separator,
            boolean indexed, boolean stored) {
        if (separator == null) {
            separator = "/";
        }
        StringBuilder sb;
        int j = -1;
        do {
            j = value.indexOf(separator, j + 1);
            if (j != -1) {
                addTermProperty(name, value.substring(0, j), indexed, false);
            }
        } while (j < value.length() && j != -1);
        // In case the "stored" stuff would be global, and last one wins
        // we put the whole last
        addTermProperty(name, value, indexed, stored);
    }

    /**
     * Builds a security property (indexing only).
     * <p>
     * Internal storage convention:
     * <ul>
     * <li> a positive ACE on perm <em>perm</em> for name <em>name</em>
     * becomes <em>+name:perm</em>
     * <li> a negative ACE becomes <em>-name:perm</em>
     * </ul>
     * This is a bijection. We don't need any escaping.
     * <p>
     * So, in the index the first character of each term value always means
     * grant or deny.
     * <p>
     * The security filtering can then be performed by using a OR statement
     * between several
     * {@link org.nuxeo.ecm.core.search.backend.compass.lucene.MatchBeforeQuery}
     * for all granting permissions.
     *
     * @param name Name of the security property
     * @param perms All permissions (including groups of perms) that grant
     *            access to the resource.
     * @param acp The full ACP to extract from
     */
    public void addSecurityProperty(String name, List<String> perms, ACP acp) {
        for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                String perm = ace.getPermission();
                if (perms.contains(perm)) {
                    String value = ace.getUsername()
                            + SecurityFiltering.SEPARATOR + perm;
                    value = ace.isGranted() ? "+" + value : "-" + value;
                    resource.addProperty(session.createProperty(name, value,
                            Property.Store.NO, // TODO temporary
                            Property.Index.UN_TOKENIZED));
                }
            }
        }
    }

    public Resource toResource() {
        return resource;
    }

}
