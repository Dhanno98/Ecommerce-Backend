package com.ecommerce.project.service;

import com.ecommerce.project.payload.AnalyticsResponse;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @InjectMocks
    AnalyticsServiceImpl analyticsService;

    @Mock
    ProductRepository productRepository;

    @Mock
    OrderRepository orderRepository;

    /// getAnalyticsData()
    @Test
    void getAnalyticsDataShouldSuccessfullyReturnAnalyticsData() {
        when(productRepository.count())
                .thenReturn(15L);

        when(orderRepository.count())
                .thenReturn(8L);

        when(orderRepository.getTotalRevenue())
                .thenReturn(BigDecimal.valueOf(2450.75));

        AnalyticsResponse result = analyticsService.getAnalyticsData();

        assertNotNull(result);
        assertEquals("15", result.getProductCount());
        assertEquals("8", result.getTotalOrders());
        assertEquals("2450.75", result.getTotalRevenue());

        verify(productRepository).count();
        verify(orderRepository).count();
        verify(orderRepository).getTotalRevenue();
    }

    @Test
    void getAnalyticsDataShouldReturnZeroRevenueWhenRepositoryReturnsNull() {
        when(productRepository.count())
                .thenReturn(15L);

        when(orderRepository.count())
                .thenReturn(8L);

        when(orderRepository.getTotalRevenue())
                .thenReturn(null);

        AnalyticsResponse result = analyticsService.getAnalyticsData();

        assertNotNull(result);
        assertEquals("15", result.getProductCount());
        assertEquals("8", result.getTotalOrders());
        assertEquals("0", result.getTotalRevenue());

        verify(productRepository).count();
        verify(orderRepository).count();
        verify(orderRepository).getTotalRevenue();
    }
}