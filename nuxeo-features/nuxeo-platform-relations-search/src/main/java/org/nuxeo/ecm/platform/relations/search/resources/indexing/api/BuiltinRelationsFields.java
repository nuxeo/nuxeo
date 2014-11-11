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

package org.nuxeo.ecm.platform.relations.search.resources.indexing.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;

/**
 * These constants hold the non prefixed names of relations
 * index fields
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public final class BuiltinRelationsFields {

    public static final String SUBJECT_URI = "subjectUri";

    public static final String OBJECT_URI = "objectUri";

    public static final String PREDICATE_URI = "predicateUri";

    /**
     * Used for the local part of the subject's URI.
     * <p>
     * This applies to {@link org.nuxeo.ecm.platform.relations.api.QNameResource} only.
     * <p>
     * In some applications, the local part of URI can be more meaningful
     * that document qid, and easier to compare with document resources than
     * the full uri, not even to mention that a document can be associated
     * to several resources.
     * <p>
     * Of course, this is intrinsically more general than document resources.
     */
    public static final String SUBJECT_URI_LOCAL = "subjectUriLocal";

    /** @see {@link SUBJECT_URI_LOCAL} for meaning & use-cases
     */
    public static final String SUBJECT_URI_NAMESPACE = "subjectUriNs";

    /** @see {@link SUBJECT_URI_LOCAL} for meaning & use-cases
     */
    public static final String OBJECT_URI_LOCAL = "objectUriLocal";

    public static final String OBJECT_URI_NAMESPACE = "objectUriNs";

    /**
     * used to store a resolved value corresponding to the resource URI.
     * <p>
     * in case of document resources, this *must* be directly comparable
     * to {@Link BuiltinDocumentFields.FIELD_DOC_QID}.
     * <p>
     * This is not filled if resolution failed.
     */
    public static final String OBJECT = "object";

    /**
     * used to store a resolved value corresponding to the resource URI.
     * <p>
     * in case of document resources, this *must* be directly comparable
     * to {@Link BuiltinDocumentFields.FIELD_DOC_QID}.
     * <p>
     * This is not filled if resolution failed.
     */
    public static final String SUBJECT = "subject";

    private static Map<String, IndexableResourceDataConf> indexableFields;

    // Utility class.
    private BuiltinRelationsFields() {
    }


    public static Map<String, IndexableResourceDataConf> getIndexableFields() {
        if (indexableFields != null) {
            return indexableFields;
        }
        indexableFields = new HashMap<String, IndexableResourceDataConf>();
        indexableFields.put(OBJECT, new IndexableFieldDescriptor(OBJECT,
                null, "keyword", true, true,
                false, false, false, null, null, null));
        indexableFields.put(SUBJECT, new IndexableFieldDescriptor(SUBJECT,
                null, "keyword", true, true,
                false, false, false, null, null, null));

        // TODO prescribe special analysis ?
        indexableFields.put(OBJECT_URI, new IndexableFieldDescriptor(OBJECT_URI,
                null, "keyword", true, true,
                false, false, false, null, null, null));

        indexableFields.put(OBJECT_URI_LOCAL,
                new IndexableFieldDescriptor(OBJECT_URI_LOCAL,
                        null, "keyword", true, true,
                        false, false, false, null, null, null));

        indexableFields.put(OBJECT_URI_NAMESPACE,
                new IndexableFieldDescriptor(OBJECT_URI_NAMESPACE,
                        null, "keyword", true, true,
                        false, false, false, null, null, null));

        indexableFields.put(SUBJECT_URI, new IndexableFieldDescriptor(
                SUBJECT_URI,
                null, "keyword", true, true,
                false, false, false, null, null, null));

        indexableFields.put(SUBJECT_URI_LOCAL,
                new IndexableFieldDescriptor(SUBJECT_URI_LOCAL,
                        null, "keyword", true, true,
                        false, false, false, null, null, null));

        indexableFields.put(SUBJECT_URI_NAMESPACE,
                new IndexableFieldDescriptor(SUBJECT_URI_NAMESPACE,
                        null, "keyword", true, true,
                        false, false, false, null, null, null));

        indexableFields.put(PREDICATE_URI, new IndexableFieldDescriptor(
                PREDICATE_URI,
                null, "keyword", true, true,
                false, false, false, null, null, null));

        return indexableFields;
    }

}
