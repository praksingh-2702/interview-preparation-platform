package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads the question bank from src/main/resources/seed/questions.psv.
 * Idempotent: only questions whose URL is not yet in the database are inserted, so new lines can be
 * appended to the file and they appear on the next deploy without touching existing data or progress.
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    static final String SEED_FILE = "seed/questions.psv";
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final QuestionRepository questionRepository;
    private final boolean enabled;

    public DatabaseSeeder(QuestionRepository questionRepository,
                          @Value("${app.seed.enabled:true}") boolean enabled) {
        this.questionRepository = questionRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }
        List<Question> seed = load();
        Set<String> existing = new HashSet<>(questionRepository.findAllProblemUrls());
        List<Question> missing = seed.stream().filter(q -> !existing.contains(q.getProblemUrl())).toList();
        if (!missing.isEmpty()) {
            questionRepository.saveAll(missing);
        }
        log.info("Question bank: {} in seed file, {} newly inserted", seed.size(), missing.size());
    }

    static List<Question> load() throws IOException {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }

    static List<Question> parse(BufferedReader reader) throws IOException {
        List<Question> result = new ArrayList<>();
        Set<String> urls = new HashSet<>();
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s*\\|\\s*", 6);
            if (f.length != 6) {
                throw new IllegalStateException(SEED_FILE + " line " + lineNo + ": expected 6 '|' separated fields");
            }
            String difficulty = f[1].toUpperCase(Locale.ROOT);
            if (!DIFFICULTIES.contains(difficulty)) {
                throw new IllegalStateException(SEED_FILE + " line " + lineNo + ": unknown difficulty '" + f[1] + "'");
            }
            if (!urls.add(f[3])) {
                throw new IllegalStateException(SEED_FILE + " line " + lineNo + ": duplicate URL " + f[3]);
            }
            Question q = new Question();
            q.setCategory(f[0]);
            q.setDifficulty(difficulty);
            q.setTitle(f[2]);
            q.setProblemUrl(f[3]);
            q.setCompanies(Arrays.stream(f[4].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(HashSet::new)));
            q.setDescription(f[5]);
            result.add(q);
        }
        return result;
    }
}
