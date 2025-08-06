package com.EOP.common_lib.common.aspect;

import com.EOP.common_lib.common.annotation.RequireDepartmentPermission;
import com.EOP.common_lib.common.enums.Department;
import com.EOP.common_lib.common.enums.Role;
import com.EOP.common_lib.common.exceptions.ForbiddenException;
import com.EOP.common_lib.common.exceptions.UnauthorizedException;
import com.EOP.common_lib.common.security.DepartmentPermissionService;
import com.EOP.common_lib.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DepartmentAuthorizationAspect {

    private final DepartmentPermissionService permissionService;
    private final JwtTokenProvider jwtTokenProvider;

    @Around("@annotation(requirePermission)")
    public Object checkDepartmentPermission(ProceedingJoinPoint joinPoint,
                                            RequireDepartmentPermission requirePermission) throws Throwable {

        HttpServletRequest request = getCurrentRequest();
        String token = extractTokenFromRequest(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid or missing authentication token");
        }

        try {
            // Extract user info from JWT
            String departmentStr = jwtTokenProvider.extractDepartment(token);
            String roleStr = jwtTokenProvider.extractRole(token);

            if (departmentStr == null || roleStr == null) {
                throw new UnauthorizedException("Token missing required claims");
            }

            Department userDepartment = Department.valueOf(departmentStr);
            Role userRole = Role.valueOf(roleStr);

            // Check role-based access first (if specified)
            if (requirePermission.allowedRoles().length > 0) {
                boolean hasRole = Arrays.asList(requirePermission.allowedRoles()).contains(userRole);
                if (!hasRole) {
                    throw new ForbiddenException("Insufficient role privileges");
                }
            }

            // Check department-based permission
            boolean hasPermission = permissionService.canAccessResource(
                    userDepartment,
                    requirePermission.service(),
                    requirePermission.action()
            );

            if (!hasPermission) {
                log.warn("Access denied for department {} trying to access {}:{}",
                        userDepartment, requirePermission.service(), requirePermission.action());
                throw new ForbiddenException(
                        String.format("Department %s does not have permission for %s:%s",
                                userDepartment, requirePermission.service(), requirePermission.action())
                );
            }

            // Add user context to request for downstream use
            request.setAttribute("userId", jwtTokenProvider.extractUserId(token));
            request.setAttribute("userDepartment", userDepartment);
            request.setAttribute("userRole", userRole);
            request.setAttribute("userEmail", jwtTokenProvider.extractEmail(token));

            log.debug("Authorization successful for user {} from department {} accessing {}:{}",
                    jwtTokenProvider.extractUserId(token), userDepartment,
                    requirePermission.service(), requirePermission.action());

            return joinPoint.proceed();

        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid token claims: " + e.getMessage());
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
