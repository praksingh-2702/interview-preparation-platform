package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotNull;

public record PreferencesRequest(@NotNull Boolean showOnLeaderboard) {
}
