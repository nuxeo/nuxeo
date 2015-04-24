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
        local time = 0 + string.sub(state, 2, -1) -- format is 'C' + completion time
        del = time < beforeTime
    end
    if del then
        redis.call('SREM', completedKey, workId)
        redis.call('HDEL', stateKey, workId)
        redis.call('HDEL', dataKey, workId)
    end
end
