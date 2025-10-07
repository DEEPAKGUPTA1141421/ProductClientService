package com.ProductClientService.ProductClientService.Service.Strategy.SearchHistoryStragecy;

import java.util.List;

public interface TrendingSearchStrategy {
    List<String> getTrending(int limit);
}