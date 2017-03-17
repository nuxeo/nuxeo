--
-- Deletes completed works with the ids given in ARGV
-- having a completion time before the given time limit.
--

local completedKey = KEYS[1]
local stateKey = KEYS[2]
local dataKey = KEYS[3]
local beforeTime = 0 + ARGV[1]
-- the rest of ARGV is the list of work ids to check and delete

for i = 2, #ARGV do
    local workId = ARGV[i]
    local del = true
    if beforeTime ~= 0 then
        local state = redis.call('HGET', stateKey, workId)
        -- if there is no state delete the key
        if state ~= false then
            assert(type(state) == "string", tostring(state) .. " is no a string")
            del = string.match(state,"C.*")
            if del then
                local stime = string.sub(state, 2) -- format is 'C' + completion time
                local time = tonumber(stime)
                del = time < beforeTime
            end
        end
    end
    if del then
        redis.call('SREM', completedKey, workId)
        redis.call('HDEL', stateKey, workId)
        redis.call('HDEL', dataKey, workId)
    end
end
