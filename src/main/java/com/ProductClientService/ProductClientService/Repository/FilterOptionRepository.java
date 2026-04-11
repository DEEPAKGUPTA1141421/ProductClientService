package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.FilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FilterOptionRepository extends JpaRepository<FilterOption, UUID> {

    List<FilterOption> findByFilterIdOrderByDisplayOrderAsc(UUID filterId);

    boolean existsByFilterIdAndOptionKey(UUID filterId, String optionKey);
}
