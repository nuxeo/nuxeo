--
-- Mark work as completed
--
local dataKey = KEYS[1]
local stateKey = KEYS[2]
local runningKey = KEYS[3]
local completedKey = KEYS[4]

local workId = ARGV[1]
local workData = ARGV[2]
local state = ARGV[3]

redis.call('HSET', dataKey, workId, workData)
redis.call('SREM', runningKey, workId)
redis.call('SADD', completedKey, workId)
redis.call('HSET', stateKey, workId, state)
