package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ASCIICharacterCheckerTest {

    private final ASCIICharacterChecker checker = new ASCIICharacterChecker();
    private final BibEntry entry = new BibEntry();

    @Test
    void fieldAcceptsAsciiCharacters() {
        entry.setField(StandardField.TITLE, "Only ascii characters!'@12");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptUmlauts() {
        entry.setField(StandardField.MONTH, "Umlauts are nöt ällowed");
        assertEquals(List.of(new IntegrityMessage("Non-ASCII encoded character found", entry, StandardField.MONTH)), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptUnicode() {
        entry.setField(StandardField.AUTHOR, "Some unicode ⊕");
        assertEquals(List.of(new IntegrityMessage("Non-ASCII encoded character found", entry, StandardField.AUTHOR)), checker.check(entry));
    }

    @Test
    void fieldAcceptsOnlyAsciiCharacters() {
        StringBuilder field = new StringBuilder();
        for (int i = 32; i <= 127; i++) {
            field.append(Character.toString(i));
        }
        entry.setField(StandardField.TITLE, field.toString());
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptNonAsciiCharacters() {
        String field = Character.toString(31) + Character.toString(128);
        entry.setField(StandardField.TITLE, field);
        assertEquals(List.of(new IntegrityMessage("Non-ASCII encoded character found", entry, StandardField.TITLE)), checker.check(entry));
    }
}
