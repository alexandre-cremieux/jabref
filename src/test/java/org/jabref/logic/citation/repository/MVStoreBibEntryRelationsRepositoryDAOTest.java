package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.citation.semanticscholar.PaperDetails;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MVStoreBibEntryRelationsRepositoryDAOTest {

    private final static String TEMPORARY_FOLDER_NAME = "bib_entry_relations_test_not_contains_store";
    private final static String MAP_NAME = "test-relations";

    @TempDir Path temporaryFolder;

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(MVStoreBibEntryRelationsRepositoryDAOTest::createBibEntry);
    }

    private static BibEntry createBibEntry(int i) {
        return new BibEntry()
            .withCitationKey(String.valueOf(i))
            .withField(StandardField.DOI, "10.1234/5678" + i);
    }

    /**
     * Create a fake list of relations for a bibEntry based on the {@link PaperDetails#toBibEntry()} logic
     * that corresponds to this use case: we want to make sure that relations coming from SemanticScholar
     * and mapped as BibEntry will be serializable by the MVStore.
     *
     * @param entry should not be null
     * @return never empty
     */
    private static List<BibEntry> createRelations(BibEntry entry) {
        return entry
            .getCitationKey()
            .map(key -> RandomGenerator.StreamableGenerator
                .of("L128X256MixRandom").ints(150)
                .mapToObj(i -> new BibEntry()
                    .withField(StandardField.TITLE, "A title:" + i)
                    .withField(StandardField.YEAR, String.valueOf(2024))
                    .withField(StandardField.AUTHOR, "A list of authors:" + i)
                    .withType(StandardEntryType.Book)
                    .withField(StandardField.DOI, entry.getDOI().map(DOI::getDOI).orElse("") + ":" + i)
                    .withField(StandardField.URL, "www.jabref.org/" + i)
                    .withField(StandardField.ABSTRACT, "The Universe is expanding:" + i)
                )
            )
            .orElseThrow()
            .collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void DAOShouldMergeRelationsWhenInserting(BibEntry bibEntry) throws IOException {
        // GIVEN
        var file = Files.createFile(temporaryFolder.resolve(TEMPORARY_FOLDER_NAME));
        var dao = new MVStoreBibEntryRelationDAO(file.toAbsolutePath(), MAP_NAME);
        Assertions.assertFalse(dao.containsKey(bibEntry));
        var firstRelations = createRelations(bibEntry);
        var secondRelations = createRelations(bibEntry);

        // WHEN
        dao.cacheOrMergeRelations(bibEntry, firstRelations);
        dao.cacheOrMergeRelations(bibEntry, secondRelations);
        var relationFromCache = dao.getRelations(bibEntry);

        // THEN
        var uniqueRelations = Stream
            .concat(firstRelations.stream(), secondRelations.stream())
            .distinct()
            .toList();
        assertFalse(uniqueRelations.isEmpty());
        assertNotSame(uniqueRelations, relationFromCache);
        assertEquals(uniqueRelations, relationFromCache);
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void containsKeyShouldReturnFalseIfNothingWasInserted(BibEntry entry) throws IOException {
        // GIVEN
        var file = Files.createFile(temporaryFolder.resolve(TEMPORARY_FOLDER_NAME));
        var dao = new MVStoreBibEntryRelationDAO(file.toAbsolutePath(), MAP_NAME);

        // THEN
        Assertions.assertFalse(dao.containsKey(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void containsKeyShouldReturnTrueIfRelationsWereInserted(BibEntry entry) throws IOException {
        // GIVEN
        var file = Files.createFile(temporaryFolder.resolve(TEMPORARY_FOLDER_NAME));
        var dao = new MVStoreBibEntryRelationDAO(file.toAbsolutePath(), MAP_NAME);
        var relations = createRelations(entry);

        // WHEN
        dao.cacheOrMergeRelations(entry, relations);

        // THEN
        Assertions.assertTrue(dao.containsKey(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void isUpdatableShouldReturnTrueBeforeInsertionsAndFalseAfterInsertions(BibEntry entry) throws IOException {
        // GIVEN
        var file = Files.createFile(temporaryFolder.resolve(TEMPORARY_FOLDER_NAME));
        var dao = new MVStoreBibEntryRelationDAO(file.toAbsolutePath(), MAP_NAME);
        var relations = createRelations(entry);
        Assertions.assertTrue(dao.isUpdatable(entry));

        // WHEN
        dao.cacheOrMergeRelations(entry, relations);

        // THEN
        Assertions.assertFalse(dao.isUpdatable(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void isUpdatableShouldReturnTrueAfterOneWeek(BibEntry entry) throws IOException {
        // GIVEN
        var file = Files.createFile(temporaryFolder.resolve(TEMPORARY_FOLDER_NAME));
        var dao = new MVStoreBibEntryRelationDAO(file.toAbsolutePath(), MAP_NAME);
        var relations = createRelations(entry);
        var clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        Assertions.assertTrue(dao.isUpdatable(entry, clock));

        // WHEN
        dao.cacheOrMergeRelations(entry, relations);

        // THEN
        Assertions.assertFalse(dao.isUpdatable(entry, clock));
        var clockOneWeekAfter = Clock.fixed(
            LocalDateTime.now(ZoneId.of("UTC")).plusWeeks(1).toInstant(ZoneOffset.UTC),
            ZoneId.of("UTC")
        );
        Assertions.assertTrue(dao.isUpdatable(entry, clockOneWeekAfter));
    }
}