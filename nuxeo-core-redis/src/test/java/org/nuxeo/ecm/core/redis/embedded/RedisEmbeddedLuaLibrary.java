package org.nuxeo.ecm.core.redis.embedded;

import java.util.Collection;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import redis.clients.jedis.Jedis;

public class RedisEmbeddedLuaLibrary extends TwoArgFunction {

    protected final Jedis jedis;

    public RedisEmbeddedLuaLibrary(Jedis jedis) {
        this.jedis = jedis;
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

    class RedisCall extends ThreeArgFunction {

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey,
                LuaValue luaArg) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode,
                    String.class);
            String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
            String arg = (String) CoerceLuaToJava.coerce(luaArg, String.class);
            switch (opcode.toLowerCase()) {
            case "get": {

                return valueOfOrFalse(jedis.get(key));
            }
            case "set": {
                return valueOfOrFalse(jedis.set(key, arg));
            }
            case "del": {
                return valueOfOrFalse(jedis.del(new String[] { key }));
            }
            case "keys": {
                LuaTable table = LuaValue.tableOf();
                int i = 0;
                for (String value : jedis.keys(key)) {
                    table.rawset(++i, LuaValue.valueOf(value));
                }
                return table;
            }
            }
            throw new UnsupportedOperationException(opcode);
        }

    }

}
