/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.FALSE;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_EACH;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_EXISTS;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_INC;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_PULLALL;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_PUSH;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_TYPE;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_UNSET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ONE;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;

import com.mongodb.client.model.Filters;

/**
 * Converts between MongoDB types (bson) and DBS types (diff, state, list, serializable).
 * <p>
 * The MongoDB native "_id" can optionally be translated into a custom id in memory (usually "ecm:id"). Otherwise it is
 * stripped from returned results.
 *
 * @since 9.1
 */
public class MongoDBConverter {

    /** The key to use in memory to map the database native "_id". */
    protected final String idKey;

    /** The keys for booleans whose value is true or null (instead of false). */
    protected final Set<String> trueOrNullBooleanKeys;

    /** The keys whose values are ids and are stored as longs. */
    protected final Set<String> idValuesKeys;

    protected boolean serverVersionBefore36;

    /**
     * Constructor for a converter that does not map the MongoDB native "_id".
     *
     * @since 10.3
     */
    public MongoDBConverter() {
        this(null, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Constructor for a converter that also knows to optionally translate the native MongoDB "_id" into a custom id.
     * <p>
     * When {@code idValuesKeys} are provided, the ids are stored as longs.
     *
     * @param idKey the key to use to map the native "_id" in memory, if not {@code null}
     * @param trueOrNullBooleanKeys the keys corresponding to boolean values that are only true or null (instead of
     *            false)
     * @param idValuesKeys the keys corresponding to values that are ids
     */
    public MongoDBConverter(String idKey, Set<String> trueOrNullBooleanKeys, Set<String> idValuesKeys) {
        this.idKey = idKey;
        this.trueOrNullBooleanKeys = trueOrNullBooleanKeys;
        this.idValuesKeys = idValuesKeys;
    }

    public void setServerVersion(String serverVersion) {
        serverVersionBefore36 = serverVersion.compareTo("3.6") < 0;
    }

    /**
     * Constructs a list of MongoDB updates from the given {@link StateDiff}.
     * <p>
     * We need a list because some cases need two operations to avoid conflicts.
     */
    public ConditionsAndUpdates diffToBson(StateDiff diff) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        return updateBuilder.build(diff);
    }

    public void putToBson(Document doc, String key, Object value) {
        doc.put(keyToBson(key), valueToBson(key, value));
    }

    public String keyToBson(String key) {
        if (idKey == null) {
            return key;
        } else {
            return idKey.equals(key) ? MONGODB_ID : key;
        }
    }

    public Object valueToBson(String key, Object value) {
        if (value instanceof State) {
            return stateToBson((State) value);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            return listToBson(key, values);
        } else if (value instanceof Object[]) {
            return listToBson(key, Arrays.asList((Object[]) value));
        } else {
            return serializableToBson(key, value);
        }
    }

    public Document stateToBson(State state) {
        Document doc = new Document();
        for (Entry<String, Serializable> en : state.entrySet()) {
            Serializable value = en.getValue();
            if (value != null) {
                putToBson(doc, en.getKey(), value);
            }
        }
        return doc;
    }

    public <T> List<Object> listToBson(String key, Collection<T> values) {
        ArrayList<Object> objects = new ArrayList<>(values.size());
        for (T value : values) {
            objects.add(valueToBson(key, value));
        }
        return objects;
    }

    public Bson filterEq(String key, Object value) {
        return Filters.eq(keyToBson(key), valueToBson(key, value));
    }

    public <T> Bson filterIn(String key, Collection<T> values) {
        return Filters.in(keyToBson(key), listToBson(key, values));
    }

    public Serializable getFromBson(Document doc, String bsonKey, String key) {
        return bsonToValue(key, doc.get(bsonKey));
    }

    public String bsonToKey(String key) {
        if (idKey == null) {
            return key;
        } else {
            return MONGODB_ID.equals(key) ? idKey : key;
        }
    }

    public State bsonToState(Document doc) {
        if (doc == null) {
            return null;
        }
        State state = new State(doc.keySet().size());
        for (String bsonKey : doc.keySet()) {
            if (idKey == null && MONGODB_ID.equals(bsonKey)) {
                // skip native id if it's not mapped to something
                continue;
            }
            String key = bsonToKey(bsonKey);
            state.put(key, getFromBson(doc, bsonKey, key));
        }
        return state;
    }

    public Serializable bsonToValue(String key, Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) {
                return null;
            } else {
                Class<?> klass = Object.class;
                for (Object o : list) {
                    if (o != null) {
                        klass = bsonToSerializableClass(key, o.getClass());
                        break;
                    }
                }
                if (Document.class.isAssignableFrom(klass)) {
                    List<Serializable> l = new ArrayList<>(list.size());
                    for (Object el : list) {
                        l.add(bsonToState((Document) el));
                    }
                    return (Serializable) l;
                } else {
                    // turn the list into a properly-typed array
                    Object[] ar = (Object[]) Array.newInstance(klass, list.size());
                    int i = 0;
                    for (Object el : list) {
                        ar[i++] = bsonToSerializable(key, el);
                    }
                    return ar;
                }
            }
        } else if (value instanceof Document) {
            return bsonToState((Document) value);
        } else {
            return bsonToSerializable(key, value);
        }
    }

    protected boolean valueIsId(String key) {
        return key != null && idValuesKeys.contains(key);
    }

    public Object serializableToBson(String key, Object value) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        if (valueIsId(key)) {
            return idToBson(value);
        }
        if (FALSE.equals(value) && key != null && trueOrNullBooleanKeys.contains(key)) {
            return null;
        }
        return value;
    }

    public Serializable bsonToSerializable(String key, Object val) {
        if (val instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) val);
            return cal;
        }
        if (valueIsId(key)) {
            return bsonToId(val);
        }
        // NXP-31148: numbers is sometime returned as Integer whereas we only deal Long
        if (val instanceof Integer) {
            return ((Integer) val).longValue();
        }
        return (Serializable) val;
    }

    public Class<?> bsonToSerializableClass(String key, Class<?> klass) {
        if (Date.class.isAssignableFrom(klass)) {
            return Calendar.class;
        }
        if (valueIsId(key)) {
            return String.class;
        }
        // NXP-31148: numbers is sometime returned as Integer whereas we only deal Long
        if (Integer.class.isAssignableFrom(klass)) {
            return Long.class;
        }
        return klass;
    }

    // exactly 16 chars in lowercase hex
    protected static final Pattern HEX_RE = Pattern.compile("[0-9a-f]{16}");

    // convert hex id to long
    protected Object idToBson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String string = (String) value;
            if (!HEX_RE.matcher(string).matches()) {
                throw new NumberFormatException(string);
            }
            return Long.parseUnsignedLong(string, 16);
        } catch (ClassCastException | NumberFormatException e) {
            return "__invalid_id__" + value;
        }
    }

    // convert long to hex id
    protected String bsonToId(Object val) {
        if (val == null) {
            return null;
        }
        try {
            String hex = Long.toHexString((Long) val);
            int nz = 16 - hex.length();
            if (nz > 0) {
                hex = "0000000000000000".substring(16 - nz) + hex;
            }
            return hex;
        } catch (ClassCastException e) {
            return "__invalid_id__" + val;
        }
    }

    public static class ConditionsAndUpdates {

        public Document conditions = new Document();

        public List<Document> updates = new ArrayList<>();
    }

    /**
     * Update list builder to prevent several updates of the same field.
     * <p>
     * This happens if two operations act on two fields where one is a prefix of the other.
     * <p>
     * Example: Cannot update 'mylist.0.string' and 'mylist' at the same time (error 16837)
     *
     * @since 5.9.5
     */
    public class UpdateBuilder {

        protected final Document set = new Document();

        protected final Document unset = new Document();

        protected final Document push = new Document();

        protected final Document pull = new Document();

        protected final Document inc = new Document();

        protected final ConditionsAndUpdates conditionsAndUpdates = new ConditionsAndUpdates();

        protected Document update;

        protected Set<String> prefixKeys;

        protected Set<String> keys;

        public ConditionsAndUpdates build(StateDiff diff) {
            processStateDiff(diff, null);
            newUpdate();
            for (Entry<String, Object> en : set.entrySet()) {
                update(MONGODB_SET, en.getKey(), en.getValue());
            }
            for (Entry<String, Object> en : unset.entrySet()) {
                update(MONGODB_UNSET, en.getKey(), en.getValue());
            }
            for (Entry<String, Object> en : push.entrySet()) {
                update(MONGODB_PUSH, en.getKey(), en.getValue());
            }
            for (Entry<String, Object> en : pull.entrySet()) {
                update(MONGODB_PULLALL, en.getKey(), en.getValue());
            }
            for (Entry<String, Object> en : inc.entrySet()) {
                update(MONGODB_INC, en.getKey(), en.getValue());
            }
            return conditionsAndUpdates;
        }

        protected void processStateDiff(StateDiff diff, String prefix) {
            String elemPrefix = prefix == null ? "" : prefix + '.';
            for (Entry<String, Serializable> en : diff.entrySet()) {
                String name = elemPrefix + en.getKey();
                Serializable value = en.getValue();
                if (value instanceof StateDiff) {
                    processStateDiff((StateDiff) value, name);
                } else if (value instanceof ListDiff) {
                    processListDiff((ListDiff) value, name);
                } else if (value instanceof Delta) {
                    processDelta((Delta) value, name);
                } else {
                    // not a diff
                    processValue(name, value);
                }
            }
        }

        protected void processListDiff(ListDiff listDiff, String prefix) {
            if (listDiff.diff != null) {
                String elemPrefix = prefix + '.';
                int i = 0;
                for (Object value : listDiff.diff) {
                    String name = elemPrefix + i;
                    if (value instanceof StateDiff) {
                        processStateDiff((StateDiff) value, name);
                    } else if (value != NOP) {
                        // set value
                        set.put(name, valueToBson(prefix, value));
                    }
                    i++;
                }
                // in order to protect against concurrent deletions of the array
                // which would make an update on foo.0.bar create a sub-document
                // instead of addressing the array element foo.0,
                // we add a condition on the type of what we're updating
                Document condition;
                if (serverVersionBefore36) {
                    // before 3.6 {$type: "array"} doesn't do what we want,
                    // so we use a simple existence check which is good enough
                    condition = new Document(MONGODB_EXISTS, ONE);
                } else {
                    condition = new Document(MONGODB_TYPE, "array");
                }
                conditionsAndUpdates.conditions.put(prefix, condition);
            }
            if (listDiff.rpush != null) {
                Object pushed;
                if (listDiff.rpush.size() == 1) {
                    // no need to use $each for one element
                    pushed = valueToBson(prefix, listDiff.rpush.get(0));
                } else {
                    pushed = new Document(MONGODB_EACH, listToBson(prefix, listDiff.rpush));
                }
                push.put(prefix, pushed);
            }
            if (listDiff.pull != null) {
                pull.put(prefix, valueToBson(prefix, listDiff.pull));
            }
        }

        protected void processDelta(Delta delta, String prefix) {
            // MongoDB can $inc a field that doesn't exist, it's treated as 0 BUT it doesn't work on null
            // so we ensure (in diffToUpdates) that we never store a null but remove the field instead
            Object incValue = valueToBson(prefix, delta.getDeltaValue());
            inc.put(prefix, incValue);
        }

        protected void processValue(String name, Serializable value) {
            if (value == null) {
                // for null values, beyond the space saving,
                // it's important to unset the field instead of setting the value to null
                // because $inc does not work on nulls but works on non-existent fields
                unset.put(name, ONE);
            } else {
                set.put(name, valueToBson(name, value));
            }
        }

        protected void newUpdate() {
            conditionsAndUpdates.updates.add(update = new Document());
            prefixKeys = new HashSet<>();
            keys = new HashSet<>();
        }

        protected void update(String op, String bsonKey, Object val) {
            checkForConflict(bsonKey);
            Document map = (Document) update.get(op);
            if (map == null) {
                update.put(op, map = new Document());
            }
            map.put(bsonKey, val);
        }

        /**
         * Checks if the key conflicts with one of the previous keys.
         * <p>
         * A conflict occurs if one key is equals to or is a prefix of the other.
         */
        protected void checkForConflict(String key) {
            List<String> pKeys = getPrefixKeys(key);
            if (conflictKeys(key, pKeys)) {
                newUpdate();
            }
            prefixKeys.addAll(pKeys);
            keys.add(key);
        }

        protected boolean conflictKeys(String key, List<String> subkeys) {
            if (prefixKeys.contains(key)) {
                return true;
            }
            for (String sk : subkeys) {
                if (keys.contains(sk)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * return a list of parents key
         * <p>
         * foo.0.bar -> [foo, foo.0, foo.0.bar]
         */
        protected List<String> getPrefixKeys(String key) {
            List<String> ret = new ArrayList<>(10);
            int i = 0;
            while ((i = key.indexOf('.', i)) > 0) {
                ret.add(key.substring(0, i++));
            }
            ret.add(key);
            return ret;
        }

    }

}
