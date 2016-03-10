--
-- Mark work as running
--
local stateKey = KEYS[1]
local scheduledKey = KEYS[2]
local runningKey = KEYS[3]

local workId = ARGV[1]
local state = ARGV[2]

redis.call('SADD', runningKey, workId)
redis.call('SREM', scheduledKey, workId)
redis.call('HSET', stateKey, workId, state)