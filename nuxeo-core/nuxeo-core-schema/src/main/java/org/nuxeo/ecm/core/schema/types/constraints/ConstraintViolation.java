/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * A constraint violation description. Use {@link #getMessage(Locale)} to get the constraint violation description.
 * <p>
 * You could customize constraint violation message using the following rules :
 * <ul>
 * <li>Use {@value #MESSAGES_KEY} key in {@value #MESSAGES_BUNDLE} bundle to customize default message</li>
 * <li>Append the constraint name to the previous key to customize the generic message to some constraint</li>
 * <li>Append the schema and the field name to the previous key to customize the message for a specific constraint
 * applied to some specific schema field.</li>
 * </ul>
 * <br>
 * For each messages, you can use parameters in the message :
 * <ul>
 * <li>The invalid value : {0}</li>
 * <li>The schema name : {1}</li>
 * <li>The field name : {2}</li>
 * <li>The constraint name : {3}</li>
 * <li>The first constraint parameter (if exists) : {4}</li>
 * <li>The second constraint parameter (if exists) : {5}</li>
 * <li>...</li>
 * </ul>
 * </p>
 * <p>
 * Examples :
 * <ul>
 * <li>label.schema.constraint.violation=Value '{0}' for field '{1}.{2}' does not respect constraint '{3}'</li>
 * <li>label.schema.constraint.violation.PatternConstraint='{1}.{2}' value ({0}) should match the following format :
 * '{4}'</li>
 * <li>label.schema.constraint.violation.PatternConstraint.myuserschema.firstname ='The firstname should not be empty'</li>
 * </ul>
 * </p>
 *
 * @since 7.1
 */
public class ConstraintViolation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Schema schema;

    private final List<PathNode> path;

    private final Constraint constraint;

    private final Object invalidValue;

    public ConstraintViolation(Schema schema, List<PathNode> fieldPath, Constraint constraint, Object invalidValue) {
        this.schema = schema;
        path = new ArrayList<PathNode>(fieldPath);
        this.constraint = constraint;
        this.invalidValue = invalidValue;
    }

    public Schema getSchema() {
        return schema;
    }

    public List<PathNode> getPath() {
        return Collections.unmodifiableList(path);
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    /**
     * @return The message if it's found in message bundles, a generic message otherwise.
     * @since 7.1
     */
    public String getMessage(Locale locale) {
        return constraint.getErrorMessage(schema, path, invalidValue, locale);
    }

    @Override
    public String toString() {
        return getMessage(Locale.ENGLISH);
    }

    /**
     * Allows to locates some constraint violation in a document.
     * <p>
     * {@link #getIndex()} are used to indicates which element violates the constraint for list properties.
     * </p>
     *
     * @since 7.1
     */
    public static class PathNode {

        private Field field;

        private boolean listItem = false;

        int index = 0;

        public PathNode(Field field) {
            this.field = field;
        }

        public PathNode(Field field, int index) {
            super();
            this.field = field;
            this.index = index;
            listItem = true;
        }

        public Field getField() {
            return field;
        }

        public int getIndex() {
            return index;
        }

        public boolean isListItem() {
            return listItem;
        }

    }

}
