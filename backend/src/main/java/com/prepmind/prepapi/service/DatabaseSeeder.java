package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.entity.Sections;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads the question banks from src/main/resources/seed/:
 *   questions.psv  DSA problems:  category | difficulty | title | url | companies | description
 *   oops.psv, os.psv, cn.psv  theory topics:  category | difficulty | topic | article url (GeeksforGeeks)
 * Idempotent: a question is inserted only if it isn't in the database yet (DSA matched by URL,
 * theory by section + title), so lines can be appended and appear on the next deploy without
 * touching existing data or anyone's progress.
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    static final String DSA_FILE = "seed/questions.psv";
    static final Map<String, String> THEORY_FILES = new LinkedHashMap<>();

    static {
        THEORY_FILES.put(Sections.OOPS, "seed/oops.psv");
        THEORY_FILES.put(Sections.OS, "seed/os.psv");
        THEORY_FILES.put(Sections.CN, "seed/cn.psv");
    }
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final QuestionRepository questionRepository;
    private final boolean enabled;

    public DatabaseSeeder(QuestionRepository questionRepository,
                          @Value("${app.seed.enabled:true}") boolean enabled) {
        this.questionRepository = questionRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }
        int tagged = questionRepository.assignDsaToUnsectioned();
        if (tagged > 0) {
            log.info("Tagged {} existing questions as DSA", tagged);
        }

        Set<String> existing = new HashSet<>();
        for (Object[] row : questionRepository.findAllSeedKeys()) {
            existing.add(key((String) row[0], (String) row[1], (String) row[2]));
        }

        List<Question> seed = loadAll();
        List<Question> missing = seed.stream()
                .filter(q -> !existing.contains(key(q.getSection(), q.getTitle(), q.getProblemUrl())))
                .toList();
        if (!missing.isEmpty()) {
            questionRepository.saveAll(missing);
        }
        log.info("Question bank: {} in seed files, {} newly inserted", seed.size(), missing.size());
    }

    /** DSA problems are identified by their practice URL; theory questions by section + title. */
    static String key(String section, String title, String url) {
        String s = section == null ? Sections.DSA : section;
        if (Sections.DSA.equals(s) && url != null) {
            return "URL|" + url;
        }
        return s + "|" + (title == null ? "" : title.trim().toLowerCase(Locale.ROOT));
    }

    static List<Question> loadAll() throws IOException {
        List<Question> all = new ArrayList<>(load());
        for (Map.Entry<String, String> e : THEORY_FILES.entrySet()) {
            all.addAll(loadTheory(e.getKey(), e.getValue()));
        }
        return all;
    }

    /** DSA problems only. */
    static List<Question> load() throws IOException {
        try (BufferedReader reader = open(DSA_FILE)) {
            return parse(reader);
        }
    }

    static List<Question> loadTheory(String section, String file) throws IOException {
        try (BufferedReader reader = open(file)) {
            return parseTheory(section, file, reader);
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
                throw new IllegalStateException(DSA_FILE + " line " + lineNo + ": expected 6 '|' separated fields");
            }
            String difficulty = difficulty(DSA_FILE, lineNo, f[1]);
            if (!urls.add(f[3])) {
                throw new IllegalStateException(DSA_FILE + " line " + lineNo + ": duplicate URL " + f[3]);
            }
            Question q = new Question();
            q.setSection(Sections.DSA);
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

    static List<Question> parseTheory(String section, String file, BufferedReader reader) throws IOException {
        List<Question> result = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        Set<String> urls = new HashSet<>();
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s*\\|\\s*");
            if (f.length != 4) {
                throw new IllegalStateException(file + " line " + lineNo
                        + ": expected 'category | difficulty | topic | url'");
            }
            if (!f[3].startsWith("https://")) {
                throw new IllegalStateException(file + " line " + lineNo + ": the link must start with https://");
            }
            if (!urls.add(f[3])) {
                throw new IllegalStateException(file + " line " + lineNo + ": duplicate URL " + f[3]);
            }
            if (!titles.add(f[2].toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException(file + " line " + lineNo + ": duplicate question " + f[2]);
            }
            Question q = new Question();
            q.setSection(section);
            q.setCategory(f[0]);
            q.setDifficulty(difficulty(file, lineNo, f[1]));
            q.setTitle(f[2]);
            q.setProblemUrl(f[3]);
            q.setCompanies(new HashSet<>());
            result.add(q);
        }
        return result;
    }

    private static String difficulty(String file, int lineNo, String raw) {
        String d = raw.toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(d)) {
            throw new IllegalStateException(file + " line " + lineNo + ": unknown difficulty '" + raw + "'");
        }
        return d;
    }

    private static BufferedReader open(String file) throws IOException {
        return new BufferedReader(new InputStreamReader(new ClassPathResource(file).getInputStream(), StandardCharsets.UTF_8));
    }
}
