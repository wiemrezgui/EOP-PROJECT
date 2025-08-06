package com.EOP.common_lib.common.security;

import com.EOP.common_lib.common.enums.Department;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DepartmentPermissionService {

    private final Map<Department, Set<String>> departmentPermissions;

    public DepartmentPermissionService() {
        departmentPermissions = new HashMap<>();
        initializePermissions();
    }

    private void initializePermissions() {
        // HR Department - Full access to jobs and candidates
        departmentPermissions.put(Department.HR, Set.of(
                // Job permissions
                "jobs:create", "jobs:read", "jobs:update", "jobs:delete", "jobs:update-status",
                // Candidate permissions
                "candidates:read", "candidates:read-by-email", "candidates:read-by-id",
                "candidates:download-resume", "candidates:read-applicants",
                // Interview permissions
                "interviews:create", "interviews:read", "interviews:update", "interviews:delete"
        ));

        // Recruitment Department - Can view jobs, manage candidates and interviews
        departmentPermissions.put(Department.RECRUITMENT, Set.of(
                // Job permissions (read only)
                "jobs:read",
                // Candidate permissions (full access)
                "candidates:read", "candidates:read-by-email", "candidates:read-by-id",
                "candidates:download-resume", "candidates:read-applicants",
                // Interview permissions
                "interviews:create", "interviews:read", "interviews:update"
        ));

        // IT Department - No access to HR/Recruitment functions
        departmentPermissions.put(Department.IT, Set.of(
                "system:read", "system:update", "users:read"
        ));

        // Management - Read access to most things for reporting
        departmentPermissions.put(Department.MANAGEMENT, Set.of(
                "jobs:read",
                "candidates:read", "candidates:read-applicants",
                "interviews:read",
                "contracts:read", "reports:read", "analytics:read"
        ));

        // Sales Department - No access to jobs/candidates
        departmentPermissions.put(Department.SALES, Set.of(
                "clients:create", "clients:read", "clients:update",
                "contracts:read", "reports:read"
        ));

        // Contracts Department
        departmentPermissions.put(Department.CONTRACTS, Set.of(
                "contracts:create", "contracts:read", "contracts:update", "contracts:delete",
                "clients:read"
        ));

        // Administration
        departmentPermissions.put(Department.ADMINISTRATION, Set.of(
                "users:read", "users:update", "system:read", "reports:read"
        ));

        // Client Relations
        departmentPermissions.put(Department.CLIENT_RELATIONS, Set.of(
                "clients:create", "clients:read", "clients:update", "clients:delete"
        ));
    }

    public boolean canAccessResource(Department department, String service, String action) {
        String permission = service + ":" + action;
        Set<String> permissions = departmentPermissions.get(department);
        return permissions != null && permissions.contains(permission);
    }

    public Set<String> getUserPermissions(Department department) {
        return departmentPermissions.getOrDefault(department, Collections.emptySet());
    }

    public boolean hasPermission(Department department, String permission) {
        Set<String> permissions = departmentPermissions.get(department);
        return permissions != null && permissions.contains(permission);
    }
}
