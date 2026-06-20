package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.impl.AdminDashboardService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardControllerTest {

    @Test
    void queryDashboardReturnsServicePayload() {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        AdminDashboardController controller = new AdminDashboardController(dashboardService);
        Map<String, Object> payload = Map.of("totalOrders", 12L, "paidOrders", 7L);
        when(dashboardService.queryDashboard()).thenReturn(payload);

        Result result = controller.queryDashboard();

        assertEquals(true, result.getSuccess());
        assertEquals(payload, result.getData());
    }
}
