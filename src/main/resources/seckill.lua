-- 1.参数列表
-- 1.1优惠卷id
local voucherId = ARGV[1]
-- 1.2用户id
local userId = ARGV[2]

-- 2.数据key
-- 2.1库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2订单key
local orderKey = 'seckill:order:' .. voucherId
-- 2.3活动时间key
local beginKey = 'seckill:begin:' .. voucherId
local endKey = 'seckill:end:' .. voucherId

-- 2.4活动时间
local beginAt = redis.call('get', beginKey)
local endAt = redis.call('get', endKey)

if (not beginAt) or (not endAt) then
    -- 活动未初始化
    return 5
end

local now = tonumber(redis.call('time')[1])
if (now < tonumber(beginAt)) then
    -- 活动未开始
    return 3
end

if (now > tonumber(endAt)) then
    -- 活动已结束
    return 4
end

-- 3.脚本业务
-- 3.1判断库存是否充足
local stock = redis.call('get',stockKey)
if (not stock) then
    -- 库存未初始化
    return 5
end

if(tonumber(stock) <= 0)then
    -- 3.2 库存不足 返回1
    return 1
end
--3.2判断用户是否下单
if(redis.call('sismember',orderKey,userId) == 1) then
    -- 3.3存在,说明是重复下单
    return 2
end
-- 3.4扣库存
redis.call('incrby',stockKey,-1)
-- 3.5下单并保存用户
redis.call('sadd',orderKey,userId)
return 0
