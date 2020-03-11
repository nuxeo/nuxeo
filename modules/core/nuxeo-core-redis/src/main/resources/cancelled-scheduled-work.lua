local dataKey = KEYS[1]
local stateKey = KEYS[2]
local countKey = KEYS[3]
local scheduledKey = KEYS[4]
local queuedKey = KEYS[5]
local runningKey = KEYS[6]
local completedKey = KEYS[7]
local canceledKey = KEYS[8]


local workId = ARGV[1]

if redis.call('LREM', queuedKey, 0, workId) == 0 then
  return {
    redis.call('HINCRBY', countKey, scheduledKey, 0),
    redis.call('HINCRBY', countKey, runningKey, 0),
    redis.call('HINCRBY', countKey, completedKey, 0),
    redis.call('HINCRBY', countKey, canceledKey, 0)
  }
end

redis.call('SREM', scheduledKey, workId)
redis.call('HDEL', stateKey, workId)
redis.call('HDEL', dataKey, workId)

return {
    redis.call('HINCRBY', countKey, scheduledKey, -1),
    redis.call('HINCRBY', countKey, runningKey, 0),
    redis.call('HINCRBY', countKey, completedKey, 0),
    redis.call('HINCRBY', countKey, canceledKey, 1)
}
