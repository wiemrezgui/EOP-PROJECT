package com.EOP.interview_service.clients;

import com.EOP.interview_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service",
        url = "${auth.service.url}" ,
        configuration = FeignClientConfig.class
)
public interface AuthServiceClient {
    @GetMapping("/api/user/check-user/{userEmail}")
    Boolean checkUserExists(
            @PathVariable String userEmail
    );
}
