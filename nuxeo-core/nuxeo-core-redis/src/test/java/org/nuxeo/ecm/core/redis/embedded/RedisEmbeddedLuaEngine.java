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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.apache.commons.codec.binary.Hex;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.script.LuaScriptEngine;
import org.luaj.vm2.script.LuaScriptEngineFactory;
import org.luaj.vm2.script.LuajContext;
import org.nuxeo.ecm.core.api.NuxeoException;

public class RedisEmbeddedLuaEngine {

    protected final Map<String, CompiledScript> binaries = new HashMap<>();

    protected final LuaScriptEngine engine;

    public RedisEmbeddedLuaEngine(RedisEmbeddedConnection connection) {
        engine = initEngine(connection);
    }

    public String load(String content) throws ScriptException {
        String md5 = md5(content);
        CompiledScript chunk = null;
        try {
            chunk = engine.compile(content);
        } catch (ScriptException cause) {
            throw cause;
        }
        binaries.put(md5, chunk);
        return md5;
    }

    protected LuaScriptEngine initEngine(RedisEmbeddedConnection connection) {
        LuaScriptEngine engine = (LuaScriptEngine) new LuaScriptEngineFactory().getScriptEngine();
        LuajContext context = (LuajContext) engine.getContext();
        LuaValue redis = context.globals.load(new RedisEmbeddedLuaLibrary(connection));
        context.globals.set("redis", redis);
        return engine;
    }

    protected String md5(String content) {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException cause) {
            throw new NuxeoException("Cannot load sha digester", cause);
        }
        byte[] key = sha.digest(content.getBytes());
        return Hex.encodeHexString(key);
    }

    public boolean exists(String key) {
        return binaries.containsKey(key);
    }

    public void flush() {
        binaries.clear();
    }

    public Object evalsha(String sha, List<String> keys, List<String> args) throws ScriptException {
        final CompiledScript script = binaries.get(sha);
        Bindings bindings = engine.createBindings();
        bindings.put("KEYS", keys.toArray(new String[keys.size()]));
        bindings.put("ARGV", args.toArray(new String[args.size()]));
        Object result = script.eval(bindings);
        return coerce(result);
    }

    public Object evalsha(byte[] sha, List<byte[]> keys, List<byte[]> args) throws ScriptException {
        String shaStr = new String(sha, StandardCharsets.US_ASCII);
        final CompiledScript script = binaries.get(shaStr);
        Bindings bindings = engine.createBindings();
        bindings.put("KEYS", keys.toArray());
        bindings.put("ARGV", args.toArray());
        Object result = script.eval(bindings);
        return coerce(result);
    }

    protected Object coerce(Object result) {
        if (result instanceof LuaValue) {
            LuaValue value = (LuaValue) result;
            if (value.isboolean() && value.toboolean() == false) {
                return null;
            }
            if (value.istable()) {
                LuaValue element = value.get(1);
                if (element.istable()) {
                    // special case for pop work script
                    return Arrays.asList(new Object[] {
                            Arrays.asList((Number[]) CoerceLuaToJava.coerce(value.get(1), Number[].class)),
                            CoerceLuaToJava.coerce(value.get(2), byte[].class) });
                }
                if (element.isnumber()) {
                    return Arrays.asList((Number[]) CoerceLuaToJava.coerce(value, Number[].class));
                }
                if (element.isstring()) {
                    return Arrays.asList((String[]) CoerceLuaToJava.coerce(value, String[].class));
                }
                throw new UnsupportedOperationException("unsupported table of " + element.typename());
            }
            return value.tojstring();
        }
        return result;
    }

}
