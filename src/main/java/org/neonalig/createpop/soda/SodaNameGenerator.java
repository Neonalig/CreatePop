package org.neonalig.createpop.soda;

import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SodaNameGenerator {
    private static final List<String> ADJECTIVES = loadTerms("data/createpop/soda_names/adjectives.txt");
    private static final List<String> NOUNS = loadTerms("data/createpop/soda_names/nouns.txt");
    private static final List<String> SUFFIXES = loadTerms("data/createpop/soda_names/suffixes.txt");

    private SodaNameGenerator() {
    }

    public static String randomName(RandomSource random) {
        String adjective = pick(ADJECTIVES, random, "Fizzy");
        String noun = pick(NOUNS, random, "Spark");
        String suffix = pick(SUFFIXES, random, "Soda");
        return adjective + " " + noun + " " + suffix;
    }

    private static String pick(List<String> terms, RandomSource random, String fallback) {
        if (terms.isEmpty()) {
            return fallback;
        }
        return terms.get(random.nextInt(terms.size()));
    }

    private static List<String> loadTerms(String resourcePath) {
        InputStream stream = SodaNameGenerator.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            return List.of();
        }

        List<String> terms = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    terms.add(trimmed);
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.copyOf(terms);
    }
}

