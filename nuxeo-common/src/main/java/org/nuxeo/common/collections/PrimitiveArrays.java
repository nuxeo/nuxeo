/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class PrimitiveArrays {

    // Utility class.
    private PrimitiveArrays() {
    }

    @SuppressWarnings({ "ObjectEquality" })
    public static Object toPrimitiveArray(Collection<Object> col, Class<?> primitiveArrayType) {
        if (primitiveArrayType == Integer.TYPE) {
            return toIntArray(col);
        } else if (primitiveArrayType == Long.TYPE) {
            return toLongArray(col);
        } else if (primitiveArrayType == Double.TYPE) {
            return toDoubleArray(col);
        } else if (primitiveArrayType == Float.TYPE) {
            return toFloatArray(col);
        } else if (primitiveArrayType == Boolean.TYPE) {
            return toBooleanArray(col);
        } else if (primitiveArrayType == Byte.TYPE) {
            return toByteArray(col);
        } else if (primitiveArrayType == Character.TYPE) {
            return toCharArray(col);
        } else if (primitiveArrayType == Short.TYPE) {
            return toShortArray(col);
        }
        return null;
    }

    public static int[] toIntArray(Collection<?> col) {
        int size = col.size();
        int[] ar = new int[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Integer) it.next();
        }
        return ar;
    }

    public static long[] toLongArray(Collection<?> col) {
        int size = col.size();
        long[] ar = new long[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Long) it.next();
        }
        return ar;
    }

    public static double[] toDoubleArray(Collection<?> col) {
        int size = col.size();
        double[] ar = new double[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Double) it.next();
        }
        return ar;
    }

    public static float[] toFloatArray(Collection<?> col) {
        int size = col.size();
        float[] ar = new float[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Float) it.next();
        }
        return ar;
    }

    public static boolean[] toBooleanArray(Collection<?> col) {
        int size = col.size();
        boolean[] ar = new boolean[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Boolean) it.next();
        }
        return ar;
    }

    public static short[] toShortArray(Collection<?> col) {
        int size = col.size();
        short[] ar = new short[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Short) it.next();
        }
        return ar;
    }

    public static byte[] toByteArray(Collection<?> col) {
        int size = col.size();
        byte[] ar = new byte[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Byte) it.next();
        }
        return ar;
    }

    public static char[] toCharArray(Collection<?> col) {
        int size = col.size();
        char[] ar = new char[size];
        Iterator<?> it = col.iterator();
        int i = 0;
        while (it.hasNext()) {
            ar[i++] = (Character) it.next();
        }
        return ar;
    }

    public static Object[] toObjectArray(Object array) {
        Class<?> arrType = array.getClass().getComponentType();
        if (arrType == null) {
            throw new IllegalArgumentException("Not an array");
        }
        if (arrType.isPrimitive()) {
            if (arrType == Integer.TYPE) {
                int[] ar = (int[]) array;
                Integer[] result = new Integer[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Long.TYPE) {
                long[] ar = (long[]) array;
                Long[] result = new Long[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Double.TYPE) {
                double[] ar = (double[]) array;
                Double[] result = new Double[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Float.TYPE) {
                float[] ar = (float[]) array;
                Float[] result = new Float[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Character.TYPE) {
                char[] ar = (char[]) array;
                Character[] result = new Character[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Byte.TYPE) {
                byte[] ar = (byte[]) array;
                Byte[] result = new Byte[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else if (arrType == Short.TYPE) {
                short[] ar = (short[]) array;
                Short[] result = new Short[ar.length];
                for (int i = 0; i < ar.length; i++) {
                    result[i] = ar[i];
                }
                return result;
            } else {
                return null;
            }
        } else {
            return (Object[]) array;
        }
    }

    public static List<?> toList(Object array) {
        Class<?> arrType = array.getClass().getComponentType();
        if (arrType.isPrimitive()) {
            if (arrType == Integer.TYPE) {
                int[] ar = (int[]) array;
                List<Integer> result = new ArrayList<>(ar.length);
                for (int v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Long.TYPE) {
                long[] ar = (long[]) array;
                List<Long> result = new ArrayList<>(ar.length);
                for (long v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Double.TYPE) {
                double[] ar = (double[]) array;
                List<Double> result = new ArrayList<>(ar.length);
                for (double v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Float.TYPE) {
                float[] ar = (float[]) array;
                List<Float> result = new ArrayList<>(ar.length);
                for (float v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Character.TYPE) {
                char[] ar = (char[]) array;
                List<Character> result = new ArrayList<>(ar.length);
                for (char v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Byte.TYPE) {
                byte[] ar = (byte[]) array;
                List<Byte> result = new ArrayList<>(ar.length);
                for (byte v : ar) {
                    result.add(v);
                }
                return result;
            } else if (arrType == Short.TYPE) {
                short[] ar = (short[]) array;
                List<Short> result = new ArrayList<>(ar.length);
                for (short v : ar) {
                    result.add(v);
                }
                return result;
            } else {
                return null;
            }
        } else {
            return Arrays.asList((Object[]) array);
        }
    }

}
