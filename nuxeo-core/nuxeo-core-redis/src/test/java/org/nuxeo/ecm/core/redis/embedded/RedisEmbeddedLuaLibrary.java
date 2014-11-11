/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.embedded;

import java.util.Collection;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

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

    class RedisCall extends LibFunction {

        @Override
        public Varargs invoke(Varargs varargs) {
            String opcode = varargs.checkjstring(1);
            switch (opcode.toLowerCase()) {

            case "del": {
                return call(varargs.arg(1), LuaValue.tableOf(varargs, 1));
            }

            }
            throw new UnsupportedOperationException(opcode);
        }

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode,
                    String.class);
            String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
            switch (opcode.toLowerCase()) {
            case "get": {
                return valueOfOrFalse(connection.get(key));
            }
            case "del": {
                return valueOfOrFalse(connection.del((String[]) CoerceLuaToJava.coerce(
                        luaKey, String[].class)));
            }
            case "keys": {
                LuaTable table = LuaValue.tableOf();
                int i = 0;
                for (String value : connection.keys(key)) {
                    table.rawset(++i, LuaValue.valueOf(value));
                }
                return table;
            }
            }
            throw new UnsupportedOperationException(opcode);
        }

        @Override
        public LuaValue call(LuaValue luaOpcode, LuaValue luaKey,
                LuaValue luaArg) {
            String opcode = (String) CoerceLuaToJava.coerce(luaOpcode,
                    String.class);
            String key = (String) CoerceLuaToJava.coerce(luaKey, String.class);
            String arg = (String) CoerceLuaToJava.coerce(luaArg, String.class);
            switch (opcode.toLowerCase()) {
            case "set": {
                return valueOfOrFalse(connection.set(key, arg));
            }
            }
            throw new UnsupportedOperationException(opcode);
        }

    }

}
