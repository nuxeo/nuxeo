--
-- Compare and set if equals
--

local key = KEYS[1]
local expected = ARGV[1]
local value = ARGV[2]

local current = redis.call('GET', key)
if current == expected then
  redis.call('SET', key, value)
  return true
else
  return false
end
