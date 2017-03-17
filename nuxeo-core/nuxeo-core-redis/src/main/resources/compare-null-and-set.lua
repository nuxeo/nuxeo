--
-- Compare to null and set if null
--

local key = KEYS[1]
local value = ARGV[1]

local current = redis.call('GET', key)
if current == false then
  redis.call('SET', key, value)
  return true
else
  return false
end
