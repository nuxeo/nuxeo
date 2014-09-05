package org.nuxeo.ecm.core.redis;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class RedisScript  extends TwoArgFunction{

    protected final LuaValue chunk;

    public RedisScript(LuaValue chunk) {
        this.chunk = chunk;
    }

    @Override
    public LuaValue call(LuaValue keys, LuaValue args) {
        return chunk.call();
    }

}
