package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepojectory extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    default User findOrCreateByPhone(String phone) {
        return findByPhone(phone).orElseGet(() -> {
            User user = new User();
            user.setPhone(phone);
            return save(user);
        });
    }
}
