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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings( { "SuppressionAnnotation" })
public class ArrayProperty extends JCRScalarProperty {

    ArrayProperty(JCRNodeProxy parent, Property property, Field field) {
        super(parent, property, field);
    }

    @Override
    protected Property create(Object value) throws DocumentException {
        try {
            return parent.getNode().setProperty(
                    field.getName().getPrefixedName(), toValueArray(value));
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set boolean property "
                    + field.getName(), e);
        }
    }

    @Override
    protected void set(Object value) throws RepositoryException,
            DocumentException {
        property.setValue(toValueArray(value));
    }

    @Override
    protected Object get() throws RepositoryException, DocumentException {
        return fromValueArray(property.getValues(), property.getType());
        // return valueArrayToList(property.getValues(), property.getType());
    }

    @SuppressWarnings("unchecked")
    private Value[] toValueArray(Object array) throws DocumentException {
        if (array == null) {
            return null;
        }
        if (array.getClass().isArray()) {
            return arrayToValueArray(array);
        } else if (array instanceof List) {
            return listToValueArray((List) array);
        }
        throw new IllegalArgumentException(String.format(
                "Unsupported type for setting list values '%s' on field '%s'",
                array, field.getName()));
    }

    private Value[] arrayToValueArray(Object array) throws DocumentException {
        ValueFactory vf;
        try {
            vf = parent.getNode().getSession().getValueFactory();
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "failed to convert value JCR multi-value on field "
                            + field.getName(), e);
        }
        // type is supposed to be list type
        Type type = field.getType();
        Type itemType = null;
        if (type.isListType()) {
            itemType = ((ListType) type).getFieldType();
        }
        Class ctype = JavaTypes.getClass(itemType);
        if (ctype == String.class) {
            String[] ar = (String[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == int.class) {
            int[] ar = (int[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Integer.class) {
            Integer[] ar = (Integer[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == long.class) {
            long[] ar = (long[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Long.class) {
            Long[] ar = (Long[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == double.class) {
            double[] ar = (double[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Double.class) {
            Double[] ar = (Double[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == float.class) {
            float[] ar = (float[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Calendar.class) {
            Calendar[] ar = (Calendar[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Date.class) {
            Date[] ar = (Date[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(ar[i]);
                values[i] = vf.createValue(cal);
            }
            return values;
        } else if (ctype == boolean.class) {
            boolean[] ar = (boolean[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Boolean.class) {
            Boolean[] ar = (Boolean[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        }
        return null;
    }

    private static Object fromValueArray(Value[] values, int type)
            throws DocumentException {
        try {
            if (type == PropertyType.STRING) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            } else if (type == PropertyType.LONG) {
                long[] ar = new long[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getLong();
                }
                return ar;
            } else if (type == PropertyType.DOUBLE) {
                double[] ar = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getDouble();
                }
                return ar;
            } else if (type == PropertyType.DATE) {
                Calendar[] ar = new Calendar[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getDate();
                }
                return ar;
            } else if (type == PropertyType.BOOLEAN) {
                boolean[] ar = new boolean[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getBoolean();
                }
                return ar;
            } else if (type == PropertyType.PATH) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            } else if (type == PropertyType.REFERENCE) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            }
            return null;
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "failed to get array value from JCR multi value", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Value[] listToValueArray(List array) throws DocumentException {
        ValueFactory vf;
        try {
            vf = parent.getNode().getSession().getValueFactory();
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "failed to convert value JCR multi value", e);
        }
        // type is supposed to be list type
        Type type = field.getType();
        Type itemType = null;
        if (type.isListType()) {
            itemType = ((ListType) type).getFieldType();
        }
        Class ctype = JavaTypes.getClass(itemType);
        if (ctype == String.class) {
            String[] ar = (String[]) array.toArray(new String[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Integer.class) {
            Integer[] ar = (Integer[]) array.toArray(new Integer[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == Long.class) {
            Long[] ar = (Long[]) array.toArray(new Long[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == Double.class) {
            Double[] ar = (Double[]) array.toArray(new Double[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].doubleValue());
            }
            return values;
        } else if (ctype == Float.class) {
            Float[] ar = (Float[]) array.toArray(new Float[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].doubleValue());
            }
            return values;
        } else if (ctype == Calendar.class) {
            Calendar[] ar = (Calendar[]) array.toArray(new Calendar[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Date.class) {
            Date[] ar = (Date[]) array.toArray(new Date[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(ar[i]);
                values[i] = vf.createValue(cal);
            }
            return values;
        } else if (ctype == Boolean.class) {
            Boolean[] ar = (Boolean[]) array.toArray(new Boolean[array.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].booleanValue());
            }
            return values;
        }
        return null;
    }

    // TODO: this method is never user. Remove?
    @Deprecated
    private static Object valueArrayToList(Value[] values, int type)
            throws DocumentException {
        try {
            if (type == PropertyType.STRING) {
                List<String> ar = new ArrayList<String>(values.length);
                for (Value value : values) {
                    ar.add(value.getString());
                }
                return ar;
            } else if (type == PropertyType.LONG) {
                List<Long> ar = new ArrayList<Long>(values.length);
                for (Value value : values) {
                    ar.add(value.getLong());
                }
                return ar;
            } else if (type == PropertyType.DOUBLE) {
                List<Double> ar = new ArrayList<Double>(values.length);
                for (Value value : values) {
                    ar.add(value.getDouble());
                }
                return ar;
            } else if (type == PropertyType.DATE) {
                List<Calendar> ar = new ArrayList<Calendar>(values.length);
                for (Value value : values) {
                    ar.add(value.getDate());
                }
                return ar;
            } else if (type == PropertyType.BOOLEAN) {
                List<Boolean> ar = new ArrayList<Boolean>(values.length);
                for (Value value : values) {
                    ar.add(value.getBoolean());
                }
                return ar;
            } else if (type == PropertyType.PATH) {
                List<String> ar = new ArrayList<String>(values.length);
                for (Value value : values) {
                    ar.add(value.getString());
                }
                return ar;
            } else if (type == PropertyType.REFERENCE) {
                List<String> ar = new ArrayList<String>(values.length);
                for (Value value : values) {
                    ar.add(value.getString());
                }
                return ar;
            }
            return null;
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "failed to get array value from JCR multi value", e);
        }
    }

}
