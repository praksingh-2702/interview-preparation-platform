package com.prepmind.prepapi.dto;

import java.util.Map;

public record AdminOverview(long totalUsers,
                            long verifiedUsers,
                            long googleUsers,
                            long newUsersLast7Days,
                            long activeUsersLast7Days,
                            Map<String, Long> usersByRole,
                            Map<String, Long> questionsBySection) {
}
