local v = redis.call('GET', KEYS[1])
if v ~= false then 
  return v
end
redis.call('SET', KEYS[1], ARGV[1]) 
return nil