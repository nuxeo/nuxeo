local v = redis.call('GET', KEYS[1])
if v == false then
  return nil
end
if ARGV[1] ~= '' then
    local i = string.find(v, ':') -- check lock owner
    if i == nil then
      return v -- error will be seen by caller
    end
    if ARGV[1] ~= string.sub(v, 1, i-1) then
      return v -- not owner
    end
end          
redis.call('DEL', KEYS) 
return v
