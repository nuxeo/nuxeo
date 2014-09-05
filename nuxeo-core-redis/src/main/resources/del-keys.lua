local keys = redis.call('keys', KEYS[1])
print(keys)
for i=1,#keys,5000
do
  local j = math.min(i+4999, #keys);
  if (i == j) then
     redis.call('del', keys[i])
  else
     print("unpack ", keys, "i = ", i, "j = ", j)
     local subkeys = table.unpack(keys, i, j)
     redis.call('del', subkeys)
  end
end