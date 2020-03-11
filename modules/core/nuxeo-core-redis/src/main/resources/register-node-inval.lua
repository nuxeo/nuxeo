--
-- Redis invalidation register a cluster node
--
local key = KEYS[1]
local startField = ARGV[1]
local startDate = ARGV[2]
local timeout = ARGV[3]

redis.call('HSET', key, startField, startDate)
redis.call('EXPIRE', key, timeout)
