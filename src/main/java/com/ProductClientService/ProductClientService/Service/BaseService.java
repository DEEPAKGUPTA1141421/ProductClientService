package com.ProductClientService.ProductClientService.Service;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;

public class BaseService {
    public UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
