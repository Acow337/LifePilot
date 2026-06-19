package com.hmdp.utils;

import com.hmdp.entity.Shop;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheClientTest {

    @Test
    void queryWithPassThroughStoresObjectJsonOnlyOnce() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:shop:1")).thenReturn(null);

        CacheClient cacheClient = new CacheClient();
        ReflectionTestUtils.setField(cacheClient, "stringRedisTemplate", redisTemplate);

        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("测试店铺");

        Shop result = cacheClient.queryWithPassThrough(
                "cache:shop:",
                1L,
                Shop.class,
                id -> shop,
                30L,
                TimeUnit.MINUTES);

        assertEquals(shop, result);
        verify(valueOperations).set(eq("cache:shop:1"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
        String cachedJson = org.mockito.Mockito.mockingDetails(valueOperations).getInvocations().stream()
                .filter(invocation -> "set".equals(invocation.getMethod().getName()))
                .map(invocation -> (String) invocation.getArgument(1))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertTrue(cachedJson.contains("\"id\":1"));
        assertTrue(cachedJson.contains("\"name\":\"测试店铺\""));
        assertFalse(cachedJson.startsWith("\""));
    }
}
