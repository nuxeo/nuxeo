/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_EACH;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_INC;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_PUSH;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_UNSET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ONE;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Convert to and from MongoDB types (bson) and DBS types (diff, state, list, serializable).
 *
 * @since 9.1
 */
public class MongoDBConverter {

    protected final String idKey;

    protected final boolean useCustomId;

    public MongoDBConverter(String idKey) {
        this.idKey = idKey;
        this.useCustomId = KEY_ID.equals(idKey);
    }

    /**
     * Constructs a list of MongoDB updates from the given {@link StateDiff}.
     * <p>
     * We need a list because some cases need two operations to avoid conflicts.
     */
    public List<DBObject> diffToBson(StateDiff diff) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        return updateBuilder.build(diff);
    }

    public String keyToBson(String key) {
        if (useCustomId) {
            return key;
        } else {
            return KEY_ID.equals(key) ? idKey : key;
        }
    }

    public Object valueToBson(Object value) {
        if (value instanceof State) {
            return stateToBson((State) value);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            return listToBson(values);
        } else if (value instanceof Object[]) {
            return listToBson(Arrays.asList((Object[]) value));
        } else {
            return serializableToBson(value);
        }
    }

    public DBObject stateToBson(State state) {
        DBObject ob = new BasicDBObject();
        for (Entry<String, Serializable> en : state.entrySet()) {
            Object val = valueToBson(en.getValue());
            if (val != null) {
                ob.put(keyToBson(en.getKey()), val);
            }
        }
        return ob;
    }

    public List<Object> listToBson(List<Object> values) {
        ArrayList<Object> objects = new ArrayList<Object>(values.size());
        for (Object value : values) {
            objects.add(valueToBson(value));
        }
        return objects;
    }

    public String bsonToKey(String key) {
        if (useCustomId) {
            return key;
        } else {
            return idKey.equals(key) ? KEY_ID : key;
        }
    }

    public State bsonToState(DBObject ob) {
        if (ob == null) {
            return null;
        }
        State state = new State(ob.keySet().size());
        for (String key : ob.keySet()) {
            if (useCustomId && MONGODB_ID.equals(key)) {
                // skip native id
                continue;
            }
            state.put(bsonToKey(key), bsonToValue(ob.get(key)));
        }
        return state;
    }

    public Serializable bsonToValue(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) {
                return null;
            } else {
                Class<?> klass = Object.class;
                for (Object o : list) {
                    if (o != null) {
                        klass = scalarToSerializableClass(o.getClass());
                        break;
                    }
                }
                if (DBObject.class.isAssignableFrom(klass)) {
                    List<Serializable> l = new ArrayList<>(list.size());
                    for (Object el : list) {
                        l.add(bsonToState((DBObject) el));
                    }
                    return (Serializable) l;
                } else {
                    // turn the list into a properly-typed array
                    Object[] ar = (Object[]) Array.newInstance(klass, list.size());
                    int i = 0;
                    for (Object el : list) {
                        ar[i++] = scalarToSerializable(el);
                    }
                    return ar;
                }
            }
        } else if (value instanceof DBObject) {
            return bsonToState((DBObject) value);
        } else {
            return scalarToSerializable(value);
        }
    }

    public Object serializableToBson(Object value) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        return value;
    }

    public Serializable scalarToSerializable(Object val) {
        if (val instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) val);
            return cal;
        }
        return (Serializable) val;
    }

    public Class<?> scalarToSerializableClass(Class<?> klass) {
        if (Date.class.isAssignableFrom(klass)) {
            return Calendar.class;
        }
        return klass;
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

        protected final BasicDBObject set = new BasicDBObject();

        protected final BasicDBObject unset = new BasicDBObject();

        protected final BasicDBObject push = new BasicDBObject();

        protected final BasicDBObject inc = new BasicDBObject();

        protected final List<DBObject> updates = new ArrayList<>(10);

        protected DBObject update;

        protected Set<String> prefixKeys;

        protected Set<String> keys;

        public List<DBObject> build(StateDiff diff) {
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
            for (Entry<String, Object> en : inc.entrySet()) {
                update(MONGODB_INC, en.getKey(), en.getValue());
            }
            return updates;
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
                String elemPrefix = prefix == null ? "" : prefix + '.';
                int i = 0;
                for (Object value : listDiff.diff) {
                    String name = elemPrefix + i;
                    if (value instanceof StateDiff) {
                        processStateDiff((StateDiff) value, name);
                    } else if (value != NOP) {
                        // set value
                        set.put(name, valueToBson(value));
                    }
                    i++;
                }
            }
            if (listDiff.rpush != null) {
                Object pushed;
                if (listDiff.rpush.size() == 1) {
                    // no need to use $each for one element
                    pushed = valueToBson(listDiff.rpush.get(0));
                } else {
                    pushed = new BasicDBObject(MONGODB_EACH, listToBson(listDiff.rpush));
                }
                push.put(prefix, pushed);
            }
        }

        protected void processDelta(Delta delta, String prefix) {
            // MongoDB can $inc a field that doesn't exist, it's treated as 0 BUT it doesn't work on null
            // so we ensure (in diffToUpdates) that we never store a null but remove the field instead
            Object incValue = valueToBson(delta.getDeltaValue());
            inc.put(prefix, incValue);
        }

        protected void processValue(String name, Serializable value) {
            if (value == null) {
                // for null values, beyond the space saving,
                // it's important to unset the field instead of setting the value to null
                // because $inc does not work on nulls but works on non-existent fields
                unset.put(name, ONE);
            } else {
                set.put(name, valueToBson(value));
            }
        }

        protected void newUpdate() {
            updates.add(update = new BasicDBObject());
            prefixKeys = new HashSet<>();
            keys = new HashSet<>();
        }

        protected void update(String op, String key, Object value) {
            checkForConflict(key);
            DBObject map = (DBObject) update.get(op);
            if (map == null) {
                update.put(op, map = new BasicDBObject());
            }
            map.put(key, value);
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
            for (String sk: subkeys) {
                if (keys.contains(sk)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * return a list of parents key
         * foo.0.bar -> [foo, foo.0, foo.0.bar]
         */
        protected List<String> getPrefixKeys(String key) {
            List<String> ret = new ArrayList<>(10);
            int i=0;
            while ((i = key.indexOf('.', i)) > 0) {
               ret.add(key.substring(0, i++));
            }
            ret.add(key);
            return ret;
        }

    }

}
