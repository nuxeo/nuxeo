--
-- Schedule a work
--
local dataKey = KEYS[1]
local stateKey = KEYS[2]
local scheduledKey = KEYS[3]
local queueKey = KEYS[4]

local workId = ARGV[1]
local workData = ARGV[2]
local state = ARGV[3]

redis.call('HSET', dataKey, workId, workData)
redis.call('SADD', scheduledKey, workId)
redis.call('HSET', stateKey, workId, state)
redis.call('LPUSH', queueKey, workId)