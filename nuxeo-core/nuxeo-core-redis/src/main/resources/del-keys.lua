local keys = redis.call('keys', KEYS[1])
for i=1,#keys,5000
do
  redis.call('del', table.unpack(keys, i, math.min(i+4999, #keys)))
end