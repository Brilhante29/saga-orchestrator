package com.portfolio.saga.domain;

import com.portfolio.saga.application.InMemorySagaLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SagaLogTest {

    @Test
    void shouldLogAndRetrieveEntries() {
        SagaLog log = new InMemorySagaLog();
        log.log("saga-1", SagaStatus.PENDING, SagaStatus.COMPLETED, "done");
        log.log("saga-1", SagaStatus.PENDING, SagaStatus.COMPLETED, "done again");
        log.log("saga-2", SagaStatus.PENDING, SagaStatus.FAILED, "failed");

        assertEquals(2, log.entries("saga-1").size());
        assertEquals(1, log.entries("saga-2").size());
        assertEquals(3, log.allEntries().size());
    }

    @Test
    void shouldClearLog() {
        SagaLog log = new InMemorySagaLog();
        log.log("saga-1", SagaStatus.PENDING, SagaStatus.COMPLETED, "done");
        log.clear();
        assertTrue(log.allEntries().isEmpty());
    }

    @Test
    void shouldHaveTimestampsOnEntries() {
        SagaLog log = new InMemorySagaLog();
        log.log("saga-1", SagaStatus.PENDING, SagaStatus.COMPLETED, "done");

        List<LogEntry> entries = log.entries("saga-1");
        assertEquals(1, entries.size());
        assertNotNull(entries.get(0).timestamp());
    }

    @Test
    void shouldReturnEmptyForUnknownSaga() {
        SagaLog log = new InMemorySagaLog();
        assertTrue(log.entries("unknown").isEmpty());
    }
}
