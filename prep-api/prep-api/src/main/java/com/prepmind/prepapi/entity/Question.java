package com.prepmind.prepapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Entity
@Table(name = "questions")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // 👈 Added this column so questions can be grouped into sorting buckets!
    @Column(nullable = false)
    private String category; 

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(name = "problem_url")
    private String problemUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_companies", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "company_name")
    private Set<String> companies;
}