package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Question;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSeederTest {

    @Test
    void bundledSeedFileIsValid() throws Exception {
        List<Question> questions = DatabaseSeeder.load();
        assertEquals(100, questions.size());

        Map<String, Long> perCategory = questions.stream()
                .collect(Collectors.groupingBy(Question::getCategory, Collectors.counting()));
        assertEquals(10, perCategory.size());
        perCategory.values().forEach(count -> assertEquals(10L, count));

        Question first = questions.get(0);
        assertEquals("Two Sum", first.getTitle());
        assertEquals(Set.of("Google", "Amazon"), first.getCompanies());
        assertTrue(questions.stream().allMatch(q -> q.getProblemUrl().startsWith("https://")));
    }

    @Test
    void rejectsMalformedLines() {
        String bad = "Arrays | EASY | Missing fields | https://x.test/\n";
        assertThrows(IllegalStateException.class,
                () -> DatabaseSeeder.parse(new BufferedReader(new StringReader(bad))));

        String badDifficulty = "Arrays | SUPER | T | https://x.test/ | A | d\n";
        assertThrows(IllegalStateException.class,
                () -> DatabaseSeeder.parse(new BufferedReader(new StringReader(badDifficulty))));
    }
}
