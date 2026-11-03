package com.prepmind.prepapi.dto;

/** JPQL projection used to read a user's progress without loading Question entities. */
public record ProgressView(Long questionId, boolean solved, boolean revised) {
}
