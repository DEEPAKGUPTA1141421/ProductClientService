package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.UserInteractionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInteractionEventRepository extends JpaRepository<UserInteractionEventEntity, Long> {
}
