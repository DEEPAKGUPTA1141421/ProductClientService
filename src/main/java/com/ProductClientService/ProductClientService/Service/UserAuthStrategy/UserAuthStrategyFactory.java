package com.ProductClientService.ProductClientService.Service.UserAuthStrategy;

import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.LoginRequest;
import com.ProductClientService.ProductClientService.Service.UserAuthStrategy.UserAuthStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserAuthStrategyFactory {

    private final Map<AuthRequest.UserType, UserAuthStrategy> strategyMap;

    public UserAuthStrategyFactory(List<UserAuthStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        UserAuthStrategy::getUserType,
                        java.util.function.Function.identity()));
    }

    public UserAuthStrategy getStrategy(String userTypeStr) {
        AuthRequest.UserType userType = AuthRequest.UserType.valueOf(userTypeStr.toUpperCase());
        UserAuthStrategy strategy = strategyMap.get(userType);
        if (strategy == null) {
            throw new IllegalArgumentException("No authentication strategy found for user type: " + userType);
        }
        return strategy;
    }
}