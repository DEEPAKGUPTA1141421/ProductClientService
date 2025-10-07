package com.ProductClientService.ProductClientService.Service.Strategy.SearchHistoryStragecy;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.Repository.UserRecentSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyTrendingSearch implements TrendingSearchStrategy {

    private final UserRecentSearchRepository repository;

    @Override
    public List<String> getTrending(int limit) {
        ZonedDateTime since = ZonedDateTime.now().minusDays(1);
        List<Object[]> results = repository.findTopMostSearchedItems(since, 10);

        return results.stream()
                .map(row -> (String) row[0])
                .toList();
    }
}
// kujijijhjiji jiljiijkjjjj