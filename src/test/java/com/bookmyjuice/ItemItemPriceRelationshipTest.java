package com.bookmyjuice;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.repository.ItemPriceRepository;
import com.bookmyjuice.repository.ItemRepository;
import com.bookmyjuice.services.ItemPriceService;
import com.bookmyjuice.services.ItemService;
import com.chargebee.models.Event;
import com.chargebee.models.Event.Content;
import com.chargebee.models.Item;
import com.chargebee.models.ItemPrice;

public class ItemItemPriceRelationshipTest {

    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private ItemPriceRepository itemPriceRepository;
    
    @Mock
    private ItemPriceService itemPriceService;
    
    @InjectMocks
    private ItemService itemService;
    
    @InjectMocks
    private ItemPriceService itemPriceServiceImpl;
    
    @Mock
    private Event event;
    
    @Mock
    private Content content;
    
    @Mock
    private Item item;
    
    @Mock
    private ItemPrice itemPrice;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testItemCreatedWithNestedItemPrice() {
        // Arrange
        String itemId = "test_item_001";
        String itemPriceId = "test_item_price_001";
        
        when(event.content()).thenReturn(content);
        when(content.item()).thenReturn(item);
        when(content.itemPrice()).thenReturn(itemPrice);
        
        when(item.id()).thenReturn(itemId);
        when(item.name()).thenReturn("Test Item");
        when(item.type()).thenReturn(Item.Type.CHARGE);
        when(item.status()).thenReturn(Item.Status.ACTIVE);
        
        when(itemPrice.id()).thenReturn(itemPriceId);
        when(itemPrice.itemId()).thenReturn(itemId);
        when(itemPrice.price()).thenReturn(1000L); // $10.00 in cents
        
        when(itemRepository.existsById(itemId)).thenReturn(false);
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());
        
        // Act
        ResponseEntity<?> result = itemService.saveItem(event);
        
        // Assert
        assertTrue(result.getStatusCode().is2xxSuccessful());
        verify(itemRepository).save(any(ItemEntity.class));
        verify(itemPriceService).saveOrUpdateItemPrice(event);
    }

    @Test
    void testItemPriceCreatedWithMissingParentItem() {
        // Arrange
        String itemId = "missing_item_001";
        String itemPriceId = "test_item_price_001";
        
        when(event.content()).thenReturn(content);
        when(content.itemPrice()).thenReturn(itemPrice);
        when(content.item()).thenReturn(null); // No item data in event
        
        when(itemPrice.id()).thenReturn(itemPriceId);
        when(itemPrice.itemId()).thenReturn(itemId);
        when(itemPrice.name()).thenReturn("Test Item Price");
        when(itemPrice.price()).thenReturn(1500L); // $15.00 in cents
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());
        when(itemPriceRepository.existsById(itemPriceId)).thenReturn(false);
        
        // Act
        boolean result = itemPriceServiceImpl.saveItemPrice(event);
        
        // Assert
        assertTrue(result);
        verify(itemRepository).save(any(ItemEntity.class)); // Parent item should be created
        verify(itemPriceRepository).save(any(ItemPriceEntity.class));
    }

    @Test
    void testItemPriceCreatedWithExistingParentItem() {
        // Arrange
        String itemId = "existing_item_001";
        String itemPriceId = "test_item_price_001";
        
        ItemEntity existingItem = new ItemEntity();
        existingItem.setId(itemId);
        existingItem.setName("Existing Item");
        existingItem.setType("charge");
        existingItem.setStatus("active");
        
        when(event.content()).thenReturn(content);
        when(content.itemPrice()).thenReturn(itemPrice);
        
        when(itemPrice.id()).thenReturn(itemPriceId);
        when(itemPrice.itemId()).thenReturn(itemId);
        when(itemPrice.name()).thenReturn("Test Item Price");
        when(itemPrice.price()).thenReturn(2000L); // $20.00 in cents
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(itemPriceRepository.existsById(itemPriceId)).thenReturn(false);
        
        // Act
        boolean result = itemPriceServiceImpl.saveItemPrice(event);
        
        // Assert
        assertTrue(result);
        verify(itemRepository, never()).save(any(ItemEntity.class)); // Should not create new item
        verify(itemPriceRepository).save(any(ItemPriceEntity.class));
    }

    @Test
    void testItemPriceUpdatedCreatesParentItemFromEventData() {
        // Arrange
        String itemId = "item_from_event_001";
        String itemPriceId = "test_item_price_001";
        
        when(event.content()).thenReturn(content);
        when(content.itemPrice()).thenReturn(itemPrice);
        when(content.item()).thenReturn(item); // Item data available in event
        
        when(itemPrice.id()).thenReturn(itemPriceId);
        when(itemPrice.itemId()).thenReturn(itemId);
        when(itemPrice.name()).thenReturn("Updated Item Price");
        
        when(item.id()).thenReturn(itemId);
        when(item.name()).thenReturn("Item From Event");
        when(item.description()).thenReturn("Description from event");
        when(item.type()).thenReturn(Item.Type.PLAN);
        when(item.status()).thenReturn(Item.Status.ACTIVE);
        when(item.enabledInPortal()).thenReturn(true);
        when(item.enabledForCheckout()).thenReturn(true);
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());
        when(itemPriceRepository.findById(itemPriceId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = itemPriceServiceImpl.updateItemPrice(event);
        
        // Assert
        assertTrue(result);
        verify(itemRepository).save(argThat(itemEntity -> 
            itemEntity.getId().equals(itemId) &&
            itemEntity.getName().equals("Item From Event") &&
            itemEntity.getDescription().equals("Description from event") &&
            itemEntity.getType().equals("PLAN")
        ));
        verify(itemPriceRepository).save(any(ItemPriceEntity.class));
    }

    @Test
    void testBidirectionalRelationshipIntegrity() {
        // Arrange
        String itemId = "relationship_test_001";
        String itemPriceId = "relationship_price_001";
        
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(itemId);
        itemEntity.setName("Relationship Test Item");
        
        ItemPriceEntity itemPriceEntity = new ItemPriceEntity();
        itemPriceEntity.setId(itemPriceId);
        itemPriceEntity.setName("Relationship Test Price");
        itemPriceEntity.setPrice(new BigDecimal("25.00"));
        
        // Act - Test bidirectional relationship setup
        itemEntity.addItemPrice(itemPriceEntity);
        
        // Assert
        assertTrue(itemEntity.getItemPrices().contains(itemPriceEntity));
        assertEquals(itemEntity, itemPriceEntity.getItem());
        assertEquals(1, itemEntity.getItemPrices().size());
        
        // Test removal
        itemEntity.removeItemPrice(itemPriceEntity);
        assertFalse(itemEntity.getItemPrices().contains(itemPriceEntity));
        assertNull(itemPriceEntity.getItem());
        assertEquals(0, itemEntity.getItemPrices().size());
    }

    @Test
    void testSaveOrUpdateItemPriceChoosesCorrectOperation() {
        // Arrange - Test when item price exists (should update)
        String itemPriceId = "existing_price_001";
        
        when(event.content()).thenReturn(content);
        when(content.itemPrice()).thenReturn(itemPrice);
        when(itemPrice.id()).thenReturn(itemPriceId);
        
        when(itemPriceRepository.existsById(itemPriceId)).thenReturn(true);
        
        // Create a spy to verify method calls
        ItemPriceService spyService = spy(itemPriceServiceImpl);
        doReturn(true).when(spyService).updateItemPrice(event);
        doReturn(true).when(spyService).saveItemPrice(event);
        
        // Act
        boolean result = spyService.saveOrUpdateItemPrice(event);
        
        // Assert
        assertTrue(result);
        verify(spyService).updateItemPrice(event);
        verify(spyService, never()).saveItemPrice(event);
        
        // Test when item price doesn't exist (should save)
        when(itemPriceRepository.existsById(itemPriceId)).thenReturn(false);
        
        result = spyService.saveOrUpdateItemPrice(event);
        
        assertTrue(result);
        verify(spyService).saveItemPrice(event);
    }
}