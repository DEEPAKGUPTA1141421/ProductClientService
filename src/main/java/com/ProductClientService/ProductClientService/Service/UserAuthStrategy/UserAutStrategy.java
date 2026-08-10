package com.ProductClientService.ProductClientService.Service.UserAuthStrategy;

import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthResult;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAutStrategy implements UserAuthStrategy {

    private final UserRepojectory userRepository;

    @Override
    public AuthRequest.UserType getUserType() {
        return AuthRequest.UserType.USER;
    }

    @Override
    public AuthResult processAuthentication(AuthRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "phone", user.getPhone(),
                "name", user.getName(),
                "status", user.getStatus());

        return new AuthResult(user.getId(), "USER", userData);
    }

    @Override
    public boolean createUser(String phone) {
        boolean isSignup = false;
        Optional<User> user = userRepository.findByPhone(phone);
        if (user.isEmpty()) {
            User newuser = new User(phone);
            userRepository.save(newuser);
            isSignup = true;
        }
        return isSignup;
    }
}
