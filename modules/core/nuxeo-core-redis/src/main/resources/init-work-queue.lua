--
-- Init queue
--
local dataKey = KEYS[1]
local stateKey = KEYS[2]
local countKey = KEYS[3]
local scheduledKey = KEYS[4]
local queuedKey = KEYS[5]
local runningKey = KEYS[6]
local completedKey = KEYS[7]
local canceledKey = KEYS[8]


local scheduledCount = redis.call('SCARD', scheduledKey)
local runningCount = redis.call('SCARD', runningKey)

return {
  redis.call('HSET', countKey, scheduledKey, scheduledCount),
  redis.call('HSET', countKey, runningKey, runningCount),
  redis.call('HINCRBY', countKey, completedKey, 0),
  redis.call('HINCRBY', countKey, canceledKey, 0)
}
