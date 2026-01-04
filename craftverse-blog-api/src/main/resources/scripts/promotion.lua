-- KEYS[1]: 유저 IP Key, KEYS[2]: 재고 Key
local user_exists = redis.call("EXISTS", KEYS[1])
if user_exists == 1 then return {-1, redis.call("GET", KEYS[2])} end -- 중복 응답 코드와 현재 재고

local stock = tonumber(redis.call("GET", KEYS[2]))
if stock <= 0 then return {-2, 0} end -- 품절 응답 코드

local remain = redis.call("DECR", KEYS[2])
redis.call("SETEX", KEYS[1], 86400, "true")
return {1, remain} -- 성공 코드와 차감 후 남은 재고