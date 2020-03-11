--
-- Deletes completed works with the ids given in ARGV
-- having a completion time before the given time limit.
--

local dataKey = KEYS[1]
local stateKey = KEYS[2]
local countKey = KEYS[3]
local scheduledKey = KEYS[4]
local queuedKey = KEYS[5]
local runningKey = KEYS[6]
local completedKey = KEYS[7]
local canceledKey = KEYS[8]


local workId = ARGV[1]
local state = ARGV[2]

-- the rest of ARGV is the list of work ids to check and delete


redis.call('SREM', runningKey, workId)
local isscheduled = redis.call('SISMEMBER', scheduledKey, workId)
if not isscheduled or isscheduled == 0 then
    redis.call('HDEL', stateKey, workId)
    redis.call('HDEL', dataKey, workId)
end

return {
    redis.call('HINCRBY', countKey, scheduledKey, 0),
    redis.call('HINCRBY', countKey, runningKey, -1),
    redis.call('HINCRBY', countKey, completedKey, 1),
    redis.call('HINCRBY', countKey, canceledKey, 0)
}
