package com.microservices.demo.analytics.service.dataaccess.repository.impl;

import com.microservices.demo.analytics.service.dataaccess.entity.AnalyticsEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyticsRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    private AnalyticsRepositoryImpl<AnalyticsEntity, UUID> repository(int batchSize) {
        AnalyticsRepositoryImpl<AnalyticsEntity, UUID> repo = new AnalyticsRepositoryImpl<>();
        repo.em = entityManager;
        repo.batchSize = batchSize;
        return repo;
    }

    private AnalyticsEntity entity() {
        return new AnalyticsEntity(UUID.randomUUID(), "word", 1L, LocalDateTime.now());
    }

    @Test
    void persist_persistsTheEntityAndReturnsItsId() {
        AnalyticsEntity entity = entity();

        UUID result = repository(50).persist(entity);

        verify(entityManager).persist(entity);
        assertThat(result).isEqualTo(entity.getId());
    }

    @Test
    void merge_delegatesDirectlyToEntityManagerMergeAndReturnsItsResult() {
        AnalyticsEntity entity = entity();
        AnalyticsEntity merged = entity();
        org.mockito.Mockito.when(entityManager.merge(entity)).thenReturn(merged);

        AnalyticsEntity result = repository(50).merge(entity);

        assertThat(result).isSameAs(merged);
    }

    @Test
    void batchPersist_emptyCollection_doesNothing() {
        repository(50).batchPersist(List.of());

        verify(entityManager, never()).persist(org.mockito.ArgumentMatchers.any());
        verify(entityManager, never()).flush();
    }

    @Test
    void batchPersist_fewerEntitiesThanBatchSize_persistsAllThenFlushesOnceAtTheEnd() {
        List<AnalyticsEntity> entities = List.of(entity(), entity(), entity());

        repository(10).batchPersist(entities);

        entities.forEach(e -> verify(entityManager).persist(e));
        verify(entityManager, org.mockito.Mockito.times(1)).flush();
        verify(entityManager, org.mockito.Mockito.times(1)).clear();
    }

    @Test
    void batchPersist_exactMultipleOfBatchSize_flushesOnceAtTheBoundaryNotTwice() {
        // regression-shaped test for the off-by-one this batching logic
        // invites: batchCnt % batchSize == 0 triggers the in-loop flush, and
        // the trailing "if not evenly divisible" check must then correctly
        // skip a SECOND flush for the same boundary.
        List<AnalyticsEntity> entities = List.of(entity(), entity());

        repository(2).batchPersist(entities);

        verify(entityManager, org.mockito.Mockito.times(1)).flush();
        verify(entityManager, org.mockito.Mockito.times(1)).clear();
    }

    @Test
    void batchPersist_multipleBatches_flushesOnceForEachFullBatchPlusOneForTheRemainder() {
        List<AnalyticsEntity> entities = List.of(entity(), entity(), entity(), entity(), entity());

        repository(2).batchPersist(entities);

        // batches of 2,2,1 -> two in-loop flushes (at counts 2 and 4) + one trailing flush for the remainder (count 5)
        verify(entityManager, org.mockito.Mockito.times(3)).flush();
        verify(entityManager, org.mockito.Mockito.times(3)).clear();
    }

    @Test
    void batchPersist_persistsBeforeFlushingForEachEntity() {
        AnalyticsEntity entity = entity();

        repository(1).batchPersist(List.of(entity));

        InOrder order = inOrder(entityManager);
        order.verify(entityManager).persist(entity);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
    }

    @Test
    void batchMerge_emptyCollection_doesNothing() {
        repository(50).batchMerge(List.of());

        verify(entityManager, never()).merge(org.mockito.ArgumentMatchers.any());
        verify(entityManager, never()).flush();
    }

    @Test
    void batchMerge_exactMultipleOfBatchSize_flushesOnceAtTheBoundaryNotTwice() {
        List<AnalyticsEntity> entities = List.of(entity(), entity());

        repository(2).batchMerge(entities);

        verify(entityManager, org.mockito.Mockito.times(1)).flush();
        verify(entityManager, org.mockito.Mockito.times(1)).clear();
    }

    @Test
    void batchMerge_mergesEveryEntity() {
        List<AnalyticsEntity> entities = List.of(entity(), entity());

        repository(50).batchMerge(entities);

        entities.forEach(e -> verify(entityManager).merge(e));
    }

    @Test
    void clear_isANoOp_doesNotTouchTheEntityManager() {
        repository(50).clear();

        verify(entityManager, never()).clear();
    }
}
