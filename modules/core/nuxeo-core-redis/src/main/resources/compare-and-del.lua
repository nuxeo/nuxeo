--
-- Compare and del if equals
--

local key = KEYS[1]
local expected = ARGV[1]

local current = redis.call('GET', key)
if current == expected then
  redis.call('DEL', key)
  return true
else
  return false
end
