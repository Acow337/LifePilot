package com.hmdp.rebbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.entity.VoucherOrder;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SECKILL_PENDING_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_RESULT_KEY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MQReceiverTest {

    @Test
    void successResultIsWrittenOnlyAfterTransactionCommit() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MQReceiver receiver = new MQReceiver();
        ReflectionTestUtils.setField(receiver, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(receiver, "objectMapper", new ObjectMapper());

        TransactionSynchronizationManager.initSynchronization();
        try {
            receiver.markSuccessAfterCommit(100L, "SUCCESS");

            verify(redisTemplate, never()).delete(SECKILL_PENDING_ORDER_KEY + 100L);
            verify(valueOperations, never()).set(eq(SECKILL_RESULT_KEY + 100L), eq("SUCCESS"), eq(1L), eq(TimeUnit.DAYS));

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());

            verify(redisTemplate).delete(SECKILL_PENDING_ORDER_KEY + 100L);
            verify(valueOperations).set(SECKILL_RESULT_KEY + 100L, "SUCCESS", 1L, TimeUnit.DAYS);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
