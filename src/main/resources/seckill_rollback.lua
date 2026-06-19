-- 回滚秒杀资格（仅在MQ投递明确失败时调用）
local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

if(redis.call('sismember', orderKey, userId) == 1) then
    redis.call('srem', orderKey, userId)
    redis.call('incrby', stockKey, 1)
end

return 0
