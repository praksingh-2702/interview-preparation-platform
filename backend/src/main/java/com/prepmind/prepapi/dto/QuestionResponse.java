package com.prepmind.prepapi.dto;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.entity.Sections;

import java.util.List;

public record QuestionResponse(
        Long id,
        String section,
        String title,
        String category,
        String description,
        String difficulty,
        String problemUrl,
        List<String> companies) {

    public static QuestionResponse from(Question q) {
        List<String> companies = q.getCompanies() == null ? List.of()
                : q.getCompanies().stream().sorted().toList();
        String section = q.getSection() == null ? Sections.DSA : q.getSection();
        return new QuestionResponse(q.getId(), section, q.getTitle(), q.getCategory(), q.getDescription(),
                q.getDifficulty(), q.getProblemUrl(), companies);
    }
}
