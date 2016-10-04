/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.redis.embedded;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.util.Collection;

public class RedisEmbeddedLuaLibrary extends TwoArgFunction {

    protected final RedisEmbeddedConnection connection;

    public RedisEmbeddedLuaLibrary(RedisEmbeddedConnection connection) {
        this.connection = connection;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("call", new RedisCall());
        env.set("redis", library);
        return library;
    }

    protected LuaValue valueOfOrFalse(Object value) {
        if (value == null) {
            return LuaValue.valueOf(false);
        }
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return CoerceJavaToLua.coerce(collection.toArray());
        }
        return CoerceJavaToLua.coerce(value);
    }

    public class RedisCall extends LibFunction {

        @Override
        public Varargs invoke(Varargs varargs) {
            String opcode = varargs.checkjstring(1);
            switch (opcode.toLowerCase()) {
            case "del":
            case "hget":
            case "hset":
            case "hincrby":
            case "hdecrby":
            case "lrem":
                return call(varargs.arg(1), LuaValue.tableOf(varargs, 1));
            }
            throw new UnsupportedOperationException(opcode);
        }

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode, String.class);
            switch (opcode.toLowerCase()) {
            case "get": {
                String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
                return valueOfOrFalse(connection.get(key));
            }
            case "del": {
                if (luaKey.istable() || luaKey.touserdata() instanceof Object[]) {
                    String[] keys = (String[]) CoerceLuaToJava.coerce(luaKey, String[].class);
                    return valueOfOrFalse(connection.del(keys));
                } else {
                    String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
                    return valueOfOrFalse(connection.del(key));
                }
            }
            case "keys": {
                String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
                LuaTable table = LuaValue.tableOf();
                int i = 0;
                for (String value : connection.keys(key)) {
                    table.rawset(++i, LuaValue.valueOf(value));
                }
                return table;
            }
            case "hget": {
                String key = (String) CoerceLuaToJava.coerce(luaKey.get(2), String.class);
                String field = (String) CoerceLuaToJava.coerce(luaKey.get(3), String.class);
                return valueOfOrFalse(connection.hget(key.getBytes(), field.getBytes()));
            }
            case "hset": {
                String key = (String) CoerceLuaToJava.coerce(luaKey.get(2), String.class);
                String field = (String) CoerceLuaToJava.coerce(luaKey.get(3), String.class);
                Object value = CoerceLuaToJava.coerce(luaKey.get(4), byte[].class);
                return valueOfOrFalse(connection.hset(key.getBytes(), field.getBytes(), (byte[]) value));
            }
            case "hincrby": {
                String key = (String) CoerceLuaToJava.coerce(luaKey.get(2), String.class);
                String field = (String) CoerceLuaToJava.coerce(luaKey.get(3), String.class);
                Long value = (Long) CoerceLuaToJava.coerce(luaKey.get(4), Long.class);
                return valueOfOrFalse(connection.hincrBy(key.getBytes(), field.getBytes(), value.longValue()));
            }
            case "lrem": {
                String key = (String) CoerceLuaToJava.coerce(luaKey.get(2), String.class);
                Long value = (Long) CoerceLuaToJava.coerce(luaKey.get(3), Long.class);
                String field = (String) CoerceLuaToJava.coerce(luaKey.get(4), String.class);
                return valueOfOrFalse(connection.lrem(key.getBytes(), value.longValue(), field.getBytes()));
            }
            case "scard": {
                String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
                return valueOfOrFalse(connection.scard(key));
            }
            case "sismember": {
                String key = (String) CoerceLuaToJava.coerce(luaKey.get(2), String.class);
                String member = (String)CoerceLuaToJava.coerce(luaKey.get(3), String.class);
                return valueOfOrFalse(connection.sismember(key, member));
            }
            case "rpop": {
                String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
                return valueOfOrFalse(connection.rpop(key));
            }
            }
            throw new UnsupportedOperationException(opcode);
        }

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey, LuaValue luaValue) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode, String.class);
            String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
            String value = (String) CoerceLuaToJava.coerce(luaValue, String.class);
            switch (opcode.toLowerCase()) {
            case "set":
                return valueOfOrFalse(connection.set(key, value));

            case "srem":
                return valueOfOrFalse(connection.srem(key, value));

            case "hdel":
                return valueOfOrFalse(connection.hdel(key, value));

            case "hget":
                return valueOfOrFalse(connection.hget(key, value));

            case "sadd":
                return valueOfOrFalse(connection.sadd(key, value));
            case "scard":
                return valueOfOrFalse(connection.scard(key));
            case "sismember":
                return valueOfOrFalse(connection.sismember(key, value));
            case "lpush":
                return valueOfOrFalse(connection.lpush(key, value));
            case "rpop": {
                return valueOfOrFalse(connection.rpop(key));
            }
            }
            throw new UnsupportedOperationException(opcode);
        }

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey, LuaValue luaArg1, LuaValue luaArg2) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode, String.class);
            String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
            switch (opcode.toLowerCase()) {
            case "hincrby": {
                String field = (String) CoerceLuaToJava.coerce(luaArg1, String.class);
                Long value = (Long) CoerceLuaToJava.coerce(luaArg2, Long.class);
                return valueOfOrFalse(connection.hincrBy(key, field, value.longValue()));
            }
            case "lrem": {
                Long value = (Long) CoerceLuaToJava.coerce(luaArg1, Long.class);
                String field = (String) CoerceLuaToJava.coerce(luaArg2, String.class);
                return valueOfOrFalse(connection.lrem(key, value.longValue(), field));
            }
            }
            throw new UnsupportedOperationException(opcode);
        }
    }

}
