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

import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_JSON;

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
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.TypesMap;
import org.nuxeo.ecm.core.storage.pgjson.PGType.PGTypeAndValue;

/**
 * Converts between PostgreSQL JSON and DBS types (diff, state, list, serializable).
 * <p>
 * Because JSON types are more limited than DBS, we need schema information (fields) when converting from a JSON number
 * to a Long, Double or Calendar.
 *
 * @since 11.1
 */
public class PGJSONConverter {

    protected final TypesMap typesMap;

    public PGJSONConverter(TypesMap typesMap) {
        this.typesMap = typesMap;
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
        return new JSONReader(json).readFullValue();
    }

    /**
     * Class reading a JSON string into a DBS {@link State}.
     * <p>
     * JSON is parsed according to strict RFC 7159. As an exception, and to conform to PostgreSQL string semantics, the
     * null character (\u0000) is not allowed.
     */
    public class JSONReader {

        protected final String json;

        protected final int length;

        protected final Deque<String> path = new ArrayDeque<>(3);

        protected int pos;

        public JSONReader(String json) {
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

        protected boolean peek(char expected) {
            if (peek() == expected) {
                read();
                return true;
            } else {
                return false;
            }
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
            if (peek('}')) {
                return null;
            }
            State state = new State();
            for (;;) {
                String key = readString();
                space();
                expect(':');
                space();
                Serializable value = readValueForKey(key);
                state.put(key, value);

                space();
                if (peek('}')) {
                    break;
                }
                expect(',', '}');
                space();
            }
            return state;
        }

        public Serializable readValueForKey(String key) {
            path.addLast(key);
            Serializable value = readValue();
            path.removeLast();
            return value;
        }

        protected List<Serializable> readArray() {
            expect('[');
            space();
            if (peek(']')) {
                return null;
            }
            List<Serializable> list = new ArrayList<>();
            for (;;) {
                Serializable value = readValueForKey(TypesMap.ARRAY_ELEM);
                list.add(value);

                space();
                if (peek(']')) {
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
            if (peek('-')) {
                // minus
                buf.append('-');
            }
            // int = zero / ( digit1-9 *DIGIT )
            if (peek('0')) {
                // zero
                buf.append('0');
            } else {
                // digit1-9
                digitFrom('1', buf);
                // *DIGIT
                digitsOptional(buf);
            }
            // frac = decimal-point 1*DIGIT
            if (peek('.')) {
                // decimal-point
                buf.append('.');
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
            Type type = typesMap.get(path);
            if (type == null) {
                // TODO don't crash, may be an old value from a previous schema
                throw exception("No type available for path: " + String.join("/", path));
            }
            if (type instanceof LongType) {
                if (num instanceof Long) {
                    return num;
                } else {
                    // TODO better behavior
                    throw exception("Got Double instead of Long for path: " + String.join("/", path));
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
                    throw exception("Got Double instead of Long for Calendar path: " + String.join("/", path));
                }
            } else {
                throw exception("Unknown type " + type.getClass().getSimpleName() + " for Number for path: "
                        + String.join("/", path));
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

    /**
     * A helper to build the PostgreSQL expression to update an existing expression by applying a diff, delta or value
     * to it.
     * <p>
     * Returns a PostgreSQL expression whose free parameters are associated to the typed {@link #values}.
     */
    public static class UpdateBuilder {

        public final List<PGTypeAndValue> values = new ArrayList<>();

        /**
         * Builds the expression as described in the class documentation.
         *
         * @param col the column to change
         * @param value the value to apply
         * @return the PostgreSQL expression
         */
        public String build(String expr, PGType type, Object value) {
            return build(expr, type, value, false);
        }

        protected String build(String expr, PGType type, Object value, boolean exprNeedsParens) {
            if (value instanceof StateDiff) {
                return buildStateDiff(expr, (StateDiff) value, exprNeedsParens);
            } else if (value instanceof ListDiff) {
                return buildListDiff(expr, type, (ListDiff) value, exprNeedsParens);
            } else if (value instanceof Delta) {
                return buildDelta(expr, type, (Delta) value);
            } else {
                return buildValue(type, value);
            }
        }

        protected String buildDelta(String expr, PGType type, Delta delta) {
            values.add(new PGTypeAndValue(type, delta.getDeltaValue()));
            return expr + " + ?";
        }

        protected String buildValue(PGType type, Object value) {
            values.add(new PGTypeAndValue(type, value));
            return type == TYPE_JSON ? "?::jsonb" : "?";
        }

        protected String buildStateDiff(String expr, StateDiff stateDiff, boolean exprNeedsParens) {
            State set = new State();
            List<String> unset = new ArrayList<>();
            String baseExpr = expr;
            for (Entry<String, Serializable> en : stateDiff.entrySet()) {
                String key = en.getKey();
                Serializable value = en.getValue();
                if (value == null) {
                    unset.add(key); // postpone unset
                } else if (value instanceof StateDiff || value instanceof ListDiff) {
                    String subExpr = baseExpr + "->'" + key + "'";
                    expr = "jsonb_set(" + expr + ", '{" + key + "}', " + build(subExpr, TYPE_JSON, value, true) + ")";
                    exprNeedsParens = false;
                } else {
                    set.put(key, value); // postpone set
                }
            }
            if (set.isEmpty() && unset.isEmpty()) {
                return expr;
            }
            StringBuilder buf = new StringBuilder();
            if (exprNeedsParens) {
                buf.append('(');
            }
            buf.append(expr);
            if (exprNeedsParens) {
                buf.append(')');
            }
            // then set/unset
            if (!set.isEmpty()) {
                buf.append(" || ?::jsonb");
                values.add(new PGTypeAndValue(TYPE_JSON, set));
            }
            if (!unset.isEmpty()) {
                if (unset.size() == 1) {
                    buf.append(" - '");
                    buf.append(unset.get(0));
                    buf.append("'");
                } else {
                    buf.append(" - '{");
                    for (String key : unset) {
                        buf.append(key);
                        buf.append(',');
                    }
                    buf.setLength(buf.length() - 1); // remove last ,
                    buf.append("}'::text[]");
                }
            }
            return buf.toString();
        }

        protected String buildListDiff(String expr, PGType type, ListDiff listDiff, boolean exprNeedsParens) {
            if (listDiff.diff != null) {
                String baseExpr = expr;
                int i = 0;
                for (Object value : listDiff.diff) {
                    if (value instanceof StateDiff) {
                        StateDiff subDiff = (StateDiff) value;
                        String subExpr = baseExpr + "->" + i;
                        expr = "jsonb_set(" + expr + ", '{" + i + "}', " + buildStateDiff(subExpr, subDiff, true) + ")";
                    } else if (value != NOP) {
                        expr = "jsonb_set(" + expr + ", '{" + i + "}', ?::jsonb)";
                        values.add(new PGTypeAndValue(TYPE_JSON, value));
                    }
                    i++;
                }
            }
            if (listDiff.rpush != null) {
                if (type == TYPE_JSON) {
                    for (Object value : listDiff.rpush) {
                        expr = "jsonb_insert(" + expr + ", '{-1}', ?::jsonb, true)";
                        values.add(new PGTypeAndValue(TYPE_JSON, value));
                    }
                } else if (type.isArray()) { // TYPE_STRING_ARRAY / TYPE_LONG_ARRAY
                    Object[] array = listDiff.rpush.toArray();
                    values.add(new PGTypeAndValue(type, array));
                    expr += " || ?";
                } else {
                    throw new UnsupportedOperationException(String.valueOf(type));
                }
            }
            return expr;
        }
    }

}
