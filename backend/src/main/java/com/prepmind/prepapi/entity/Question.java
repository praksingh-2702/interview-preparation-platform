package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** EASY, MEDIUM or HARD. */
    @Column(nullable = false)
    private String difficulty;

    /** Where the question is studied: LeetCode for DSA, a GeeksforGeeks article for theory sections. */
    @Column(name = "problem_url")
    private String problemUrl;

    /** DSA, OOPS, OS or CN (see Sections). Null only for rows created before v3; backfilled to DSA at startup. */
    @Column(length = 10)
    private String section = Sections.DSA;

    @ElementCollection
    @CollectionTable(name = "question_companies", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "company_name")
    private Set<String> companies = new HashSet<>();
}
