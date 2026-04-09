package com.bookmyjuice.services;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.bookmyjuice.models.entities.SubscriptionItemEntity;
import com.bookmyjuice.repository.SubscriptionItemEntityRepository;

class SubscriptionItemServiceTest {
    @Mock
    private SubscriptionItemEntityRepository subscriptionItemRepository;

    @InjectMocks
    private SubscriptionItemService subscriptionItemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSave() {
        SubscriptionItemEntity item = new SubscriptionItemEntity();
        when(subscriptionItemRepository.save(item)).thenReturn(item);
        SubscriptionItemEntity result = subscriptionItemService.save(item);
        assertEquals(item, result);
        verify(subscriptionItemRepository, times(1)).save(item);
    }

    @Test
    void testFindBySubscriptionId() {
        String subscriptionId = "sub_123";
        SubscriptionItemEntity item1 = new SubscriptionItemEntity();
        SubscriptionItemEntity item2 = new SubscriptionItemEntity();
        List<SubscriptionItemEntity> items = Arrays.asList(item1, item2);
        when(subscriptionItemRepository.findBySubscription_Id(subscriptionId)).thenReturn(items);
        List<SubscriptionItemEntity> result = subscriptionItemService.findBySubscriptionId(subscriptionId);
        assertEquals(2, result.size());
        verify(subscriptionItemRepository, times(1)).findBySubscription_Id(subscriptionId);
    }

    @Test
    void testDeleteById() {
        String itemPriceId = "item_456";
        doNothing().when(subscriptionItemRepository).deleteById(itemPriceId);
        subscriptionItemService.deleteById(itemPriceId);
        verify(subscriptionItemRepository, times(1)).deleteById(itemPriceId);
    }
}
