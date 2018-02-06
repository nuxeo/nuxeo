--
-- Add a representation of a Nuxeo Document into Redis
--

local docKey = KEYS[1]
local dataKey = KEYS[2]
local folderKey = KEYS[3]

local key = ARGV[1]
local parentPath = ARGV[2]
local type = ARGV[3]
local name = ARGV[4]
local payload = ARGV[5]
local url = ARGV[6]
local level = ARGV[7]

if type == 'Folder' then
    redis.call('ZADD', folderKey, level, key)
else
    redis.call('SADD', docKey, key)
end
redis.call('HMSET', dataKey, 'key', key)
redis.call('HMSET', dataKey, 'parentPath', parentPath)
redis.call('HMSET', dataKey, 'type', type)
redis.call('HMSET', dataKey, 'name', name)
redis.call('HMSET', dataKey, 'payload', payload)
redis.call('HMSET', dataKey, 'url', url)
