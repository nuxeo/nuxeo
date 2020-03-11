local dataKey = KEYS[1]
local stateKey = KEYS[2]
local countKey = KEYS[3]
local scheduledKey = KEYS[4]
local queuedKey = KEYS[5]
local runningKey = KEYS[6]
local completedKey = KEYS[7]
local canceledKey = KEYS[8]


local id = ARGV[1]
local state = ARGV[2]
local data = ARGV[3]

if redis.call('SREM', runningKey, id) == 0 then
  return {
    redis.call('HINCRBY', countKey, scheduledKey, 0),
    redis.call('HINCRBY', countKey, runningKey, 0),
    redis.call('HINCRBY', countKey, completedKey, 0),
    redis.call('HINCRBY', countKey, canceledKey, 0)
  }
end

redis.call('SADD', scheduledKey, id)
redis.call('HSET', stateKey, id, state)
redis.call('HSET', dataKey, id, data)
redis.call('LPUSH', queuedKey, id)

return {
    redis.call('HINCRBY', countKey, scheduledKey, 1),
    redis.call('HINCRBY', countKey, runningKey, -1),
    redis.call('HINCRBY', countKey, completedKey, 0),
    redis.call('HINCRBY', countKey, canceledKey, 1)
}
