--
-- Pop a work
--
local dataKey = KEYS[1]
local stateKey = KEYS[2]
local countKey = KEYS[3]
local scheduledKey = KEYS[4]
local queuedKey = KEYS[5]
local runningKey = KEYS[6]
local completedKey = KEYS[7]
local canceledKey = KEYS[8]

local state = ARGV[1]

local id = redis.call('RPOP', queuedKey)
if (id == false) then
  return false
end

local isrunning = redis.call('SISMEMBER', runningKey, id)
-- weird check because of embedded lua that returns a boolean instead of integer
if isrunning == 1 or isrunning == true then
    redis.call('LPUSH', queuedKey, id)
    return false
end

redis.call('SREM', scheduledKey, id)
redis.call('SADD', runningKey, id)
redis.call('HSET', stateKey, id, state)

return {
    {
      redis.call('HINCRBY', countKey, scheduledKey, -1),
      redis.call('HINCRBY', countKey, runningKey, 1),
      redis.call('HINCRBY', countKey, completedKey, 0),
      redis.call('HINCRBY', countKey, canceledKey, 0)
    },
    redis.call('HGET', dataKey, id)
}

