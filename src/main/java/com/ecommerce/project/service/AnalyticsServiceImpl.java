package com.ecommerce.project.service;

import com.ecommerce.project.payload.AnalyticsResponse;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProductRepository productRepository;

    private final OrderRepository orderRepository;

    @Override
    public AnalyticsResponse getAnalyticsData() {
        AnalyticsResponse analyticsResponse = new AnalyticsResponse();

        Long productCount = productRepository.count();
        Long totalOrders = orderRepository.count();
        Double totalRevenue = orderRepository.getTotalRevenue();

        analyticsResponse.setProductCount(String.valueOf(productCount));
        analyticsResponse.setTotalOrders(String.valueOf(totalOrders));
        analyticsResponse.setTotalRevenue(String.valueOf(totalRevenue != null ? totalRevenue : 0));
        return analyticsResponse;
    }
}
