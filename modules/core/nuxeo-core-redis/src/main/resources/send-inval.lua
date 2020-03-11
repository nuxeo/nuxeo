--
-- Send VCS invalidations
--
local channel = KEYS[1]
local key = KEYS[2]

local invals = ARGV[1]
local startField = ARGV[2]
local startDate = ARGV[3]
local lastInvalField = ARGV[4]
local invalDate = ARGV[5]
local timeout = ARGV[6]

redis.call('PUBLISH', channel, invals)
redis.call('HSET', key, startField, startDate)
redis.call('HSET', key, lastInvalField, invalDate)
redis.call('EXPIRE', key, timeout)
