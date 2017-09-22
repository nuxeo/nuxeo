local keys = redis.call('keys', KEYS[1])
local unpack = unpack or table.unpack
for i=1,#keys,5000
do
  redis.call('del', unpack(keys, i, math.min(i+4999, #keys)))
end