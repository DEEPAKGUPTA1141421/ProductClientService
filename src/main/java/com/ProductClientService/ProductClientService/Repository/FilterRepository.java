package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.Filter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilterRepository extends JpaRepository<Filter, UUID> {

    Optional<Filter> findByFilterKey(String filterKey);

    boolean existsByFilterKey(String filterKey);

    /** All global filters ordered for consistent rendering. */
    List<Filter> findByIsGlobalTrueOrderByDisplayOrderAsc();

    /** All filters for admin list view. */
    List<Filter> findAllByOrderByDisplayOrderAsc();
}
