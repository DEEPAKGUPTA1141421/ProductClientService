package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.UserAffinitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserAffinitySnapshotRepository extends JpaRepository<UserAffinitySnapshot, UUID> {
}
