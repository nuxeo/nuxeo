/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.pgjson;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.storage.State;

/**
 * Converts between PostgreSQL JSON and DBS types (diff, state, list, serializable).
 * <p>
 * Because JSON types are more limited than DBS, we need schema information (fields) when converting from a JSON number
 * to a Long, Double or Calendar.
 *
 * @since 11.1
 */
public class PGJSONConverter {

    protected final Map<String, Type> types;

    public PGJSONConverter(Map<String, Type> types) {
        this.types = types;
    }

    // ========== DBS to JSON ==========

    /**
     * Converts a value from DBS into a JSON string.
     */
    public String valueToJson(Object value) {
        JSONWriter writer = new JSONWriter();
        writer.valueToJson(value);
        return writer.getJson();
    }

    /**
     * Class writing a DBS {@link State} into a JSON string.
     */
    public static class JSONWriter {

        protected final StringBuilder buf = new StringBuilder();

        public String getJson() {
            return buf.toString();
        }

        public void valueToJson(Object value) {
            if (value instanceof State) {
                stateToJson((State) value);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) value;
                listToJson(values);
            } else if (value instanceof Object[]) {
                listToJson(Arrays.asList((Object[]) value));
            } else {
                serializableToJson(value);
            }
        }

        public void stateToJson(State state) {
            buf.append('{');
            boolean first = true;
            for (Entry<String, Serializable> en : state.entrySet()) {
                String key = en.getKey();
                Serializable value = en.getValue();
                if (value == null) {
                    // we don't write null values
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                stringToJson(key);
                buf.append(':');
                valueToJson(value);
            }
            buf.append('}');
        }

        public void listToJson(List<Object> values) {
            buf.append('[');
            boolean first = true;
            for (Object value : values) {
                if (value == null) {
                    // we don't write null values
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                valueToJson(value);
            }
            buf.append(']');
        }

        public void serializableToJson(Object value) {
            if (value instanceof String) {
                stringToJson((String) value);
            } else if (value instanceof Long || value instanceof Double || value instanceof Boolean) {
                buf.append(value);
            } else if (value instanceof Calendar) {
                long millis = ((Calendar) value).getTimeInMillis();
                buf.append(millis);
            } else {
                throw new NuxeoException("Unsupported type: " + value.getClass().getName());
            }
        }

        public void stringToJson(String string) {
            buf.append('"');
            int length = string.length();
            for (int pos = 0; pos < length; pos++) {
                char c = string.charAt(pos);
                if (c == 0) {
                    throw new NuxeoException("Invalid null character at pos: " + pos);
                } else if (c == '"' || c == '\\') {
                    buf.append('\\');
                    buf.append(c);
                } else if (c == '\b') {
                    buf.append('\\');
                    buf.append('b');
                } else if (c == '\f') {
                    buf.append('\\');
                    buf.append('f');
                } else if (c == '\n') {
                    buf.append('\\');
                    buf.append('n');
                } else if (c == '\r') {
                    buf.append('\\');
                    buf.append('r');
                } else if (c == '\t') {
                    buf.append('\\');
                    buf.append('t');
                } else if (c >= ' ' && c <= '~') {
                    buf.append(c);
                } else {
                    buf.append('\\');
                    buf.append('u');
                    buf.append(hex((c >> 12) & 0xF));
                    buf.append(hex((c >> 8) & 0xF));
                    buf.append(hex((c >> 4) & 0xF));
                    buf.append(hex(c & 0xF));
                }
            }

            buf.append('"');
        }

        protected char hex(int v) {
            if (v < 10) {
                return (char) ('0' + v);
            } else {
                return (char) ('A' + v - 10);
            }
        }
    }

    /**
     * Converts a value from DBS into a database column value.
     */
    public Serializable valueToColumn(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String || value instanceof Long || value instanceof Double
                || value instanceof Boolean) {
            return (Serializable) value;
        } else if (value instanceof String[] || value instanceof Long[]) {
            return (Serializable) value;
        } else if (value instanceof Calendar) {
            long millis = ((Calendar) value).getTimeInMillis();
            return Long.valueOf(millis);
        } else {
            throw new NuxeoException("Unsupported type: " + value.getClass().getName());
        }
    }

    // ========== JSON to State ==========

    /**
     * Converts a JSON string to a DBS value.
     */
    public Serializable jsonToValue(String json) {
        return new JSONReader(types, json).readFullValue();
    }

    /**
     * Class reading a JSON string into a DBS {@link State}.
     * <p>
     * JSON is parsed according to strict RFC 7159. As an exception, and to conform to PostgreSQL string semantics, the
     * null character (\u0000) is not allowed.
     */
    public static class JSONReader {

        protected final Map<String, Type> types;

        protected final String json;

        protected final int length;

        protected final Deque<String> path = new ArrayDeque<>(3);

        protected int pos;

        public JSONReader(Map<String, Type> types, String json) {
            this.types = types;
            this.json = json;
            this.length = json.length();
        }

        protected NuxeoException exception(String message) {
            return new NuxeoException(message);
        }

        protected void error(char actual, char expected, char other) {
            if (actual == 0) {
                if (other == 0) {
                    throw exception("Expected '" + expected + "' instead of EOF at pos: " + pos);
                } else {
                    throw exception("Expected '" + expected + "' or '" + other + "' instead of EOF at pos: " + pos);
                }
            } else if (expected == 0) {
                throw exception("Expected EOF instead of '" + actual + "' at pos: " + pos);
            } else {
                if (other == 0) {
                    throw exception("Expected '" + expected + "' instead of '" + actual + "' at pos: " + pos);
                } else {
                    throw exception("Expected '" + expected + "' or '" + other + "' instead of '" + actual
                            + "' at pos: " + pos);
                }
            }
        }

        protected void expect(char expected, char other) {
            char actual = read();
            if (actual != expected) {
                error(actual, expected, other);
            }
        }

        protected void expect(char expected) {
            char actual = read();
            if (actual != expected) {
                error(actual, expected, (char) 0);
            }
        }

        protected NuxeoException unexpected(char actual) {
            if (actual == 0) {
                return exception("Unexpected EOF at pos: " + pos);
            } else {
                return exception("Unexpected '" + actual + "' at pos: " + pos);
            }
        }

        protected char peek() {
            if (pos >= length) {
                return 0;
            }
            return json.charAt(pos);
        }

        protected char read() {
            if (pos >= length) {
                return 0;
            }
            return json.charAt(pos++);
        }

        protected void space() {
            char c;
            while ((c = peek()) == ' ' || c == '\n' || c == '\r' || c == '\t') {
                pos++;
            }
        }

        public Serializable readFullValue() {
            Serializable value = readValue();
            expect((char) 0);
            return value;
        }

        public Serializable readValue() {
            char c = peek();
            Serializable value;
            if (c == 0) {
                throw exception("Expected value instead of EOF at pos: " + pos);
            } else if (c == '{') {
                value = readObject();
            } else if (c == '[') {
                value = convertList(readArray());
            } else if (c == '"') {
                value = readString();
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                value = convertNumber(readNumber());
            } else if (c == 't') {
                value = readTrue();
            } else if (c == 'f') {
                value = readFalse();
            } else if (c == 'n') {
                value = readNull();
            } else {
                throw unexpected(read());
            }
            return value;
        }

        public State readObject() {
            expect('{');
            space();
            if (peek() == '}') {
                read();
                return null;
            }
            State state = new State();
            for (;;) {
                String key = readString();
                space();
                expect(':');
                space();
                path.addLast(key);
                Serializable value = readValue();
                path.removeLast();
                state.put(key, value);

                space();
                if (peek() == '}') {
                    read();
                    break;
                }
                expect(',', '}');
                space();
            }
            return state;
        }

        protected List<Serializable> readArray() {
            expect('[');
            space();
            if (peek() == ']') {
                read();
                return null;
            }
            List<Serializable> list = new ArrayList<>();
            for (;;) {
                Serializable value = readValue();
                list.add(value);

                space();
                if (peek() == ']') {
                    read();
                    break;
                }
                expect(',', ']');
                space();
            }
            return list;
        }

        protected String readString() {
            expect('"');
            StringBuilder buf = new StringBuilder();
            for (;;) {
                char c = read();
                if (c == 0) {
                    throw exception("Unexpected EOF in string at pos: " + pos);
                } else if (c == '"') {
                    break;
                } else if (c == '\\') {
                    char n = read();
                    if (n == 0) {
                        throw exception("Unexpected EOF in string escape at pos: " + pos);
                    } else if (n == '"' || n == '\\' || n == '/') {
                        c = n;
                    } else if (n == 'b') {
                        c = '\b';
                    } else if (n == 'f') {
                        c = '\f';
                    } else if (n == 'n') {
                        c = '\n';
                    } else if (n == 'r') {
                        c = '\r';
                    } else if (n == 't') {
                        c = '\t';
                    } else if (n == 'u') {
                        c = (char) (readHex() << 12 | readHex() << 8 | readHex() << 4 | readHex());
                    } else {
                        throw exception("Invalid escape '" + n + "' at pos: " + pos);
                    }
                }
                buf.append(c);
            }
            return buf.toString();
        }

        protected int readHex() {
            char c = read();
            if (c >= '0' && c <= '9') {
                return c - '0';
            } else if (c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            } else if (c == 0) {
                throw exception("Unexpected EOF in string hex escape at pos: " + pos);
            } else {
                throw exception("Invalid hex escape '" + c + "' at pos: " + pos);
            }
        }

        protected Number readNumber() {
            // we read into a string then let Java convert to what's best
            StringBuilder buf = new StringBuilder();
            // number = [ minus ] int [ frac ] [ exp ]
            // minus
            if (peek() == '-') {
                // minus
                buf.append(read());
            }
            // int = zero / ( digit1-9 *DIGIT )
            if (peek() == '0') {
                // zero
                buf.append(read());
            } else {
                // digit1-9
                digitFrom('1', buf);
                // *DIGIT
                digitsOptional(buf);
            }
            // frac = decimal-point 1*DIGIT
            if (peek() == '.') {
                // decimal-point
                buf.append(read());
                // DIGIT
                digitFrom('0', buf);
                // *DIGIT
                digitsOptional(buf);
            }
            // exp = e [ minus / plus ] 1*DIGIT
            char e;
            if ((e = peek()) == 'e' || e == 'E') {
                // e
                buf.append(read());
                // [ minus / plus ]
                char mp = peek();
                if (mp == '-' || mp == '+') {
                    buf.append(read());
                }
                // DIGIT
                digitFrom('0', buf);
                // *DIGIT
                digitsOptional(buf);
            }

            // convert to Java
            String string = buf.toString();
            try {
                return Long.valueOf(string);
            } catch (NumberFormatException ee) {
                try {
                    return Double.valueOf(string);
                } catch (NumberFormatException eee) {
                    throw exception("Cannot parse number '" + string + "' at pos: " + (pos - string.length()));
                }
            }
        }

        protected void digitFrom(char min, StringBuilder buf) {
            char c = read();
            if (c >= min && c <= '9') {
                buf.append(c);
            } else {
                if (c == 0) {
                    throw exception("Expected digit between '" + min + "' and '9' instead of EOF at pos: " + pos);
                } else {
                    throw exception(
                            "Expected digit between '" + min + "' and '9' instead of '" + c + "' at pos: " + pos);
                }
            }
        }

        protected void digitsOptional(StringBuilder buf) {
            for (;;) {
                char c = peek();
                if (c >= '0' && c <= '9') {
                    buf.append(read());
                } else {
                    break;
                }
            }
        }

        protected Boolean readTrue() {
            expect('t');
            expect('r');
            expect('u');
            expect('e');
            return Boolean.TRUE;
        }

        protected Boolean readFalse() {
            expect('f');
            expect('a');
            expect('l');
            expect('s');
            expect('e');
            return Boolean.FALSE;
        }

        protected Serializable readNull() {
            expect('n');
            expect('u');
            expect('l');
            expect('l');
            return null;
        }

        protected Serializable convertNumber(Number num) {
            String xpath = String.join("/", path);
            Type type = types.get(xpath);
            if (type == null) {
                // TODO don't crash, may be an old value from a previous schema
                throw exception("No type available for path: " + xpath);
            }
            if (type instanceof LongType) {
                if (num instanceof Long) {
                    return num;
                } else {
                    // TODO better behavior
                    throw exception("Got Double instead of Long for path: " + xpath);
                }
            } else if (type instanceof DoubleType) {
                if (num instanceof Double) {
                    return num;
                } else {
                    return Double.valueOf(((Long) num).longValue());
                }
            } else if (type instanceof DateType) {
                if (num instanceof Long) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(((Long) num).longValue());
                    return cal;
                } else {
                    // TODO better behavior
                    throw exception("Got Double instead of Long for Calendar path: " + xpath);
                }
            } else {
                throw exception("Unknown type " + type.getClass().getSimpleName() + " for Number for path: " + xpath);
            }
        }

        protected Serializable convertList(List<Serializable> list) {
            if (list == null || list.isEmpty()) {
                return null;
            }
            Serializable first = list.get(0);
            if (first instanceof State) {
                // keep the list
                return (Serializable) list;
            } else {
                // turn it into an array
                Object[] array = (Object[]) Array.newInstance(first.getClass(), list.size());
                return list.toArray(array);
            }
        }
    }

}
