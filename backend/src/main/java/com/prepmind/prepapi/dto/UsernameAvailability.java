package com.prepmind.prepapi.dto;

/** available=false comes with a human-readable reason (taken, too short, bad characters...). */
public record UsernameAvailability(boolean available, String message) {
}
