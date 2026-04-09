package com.bookmyjuice.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.models.mappers.CustomerMapper;
import com.bookmyjuice.models.mappers.ItemMapper;
import com.bookmyjuice.models.mappers.OrderMapper;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.ItemPriceRepository;
import com.bookmyjuice.repository.ItemRepository;
import com.bookmyjuice.repository.OrderRepository;
import com.chargebee.ListResult;
import com.chargebee.models.Customer;
import com.chargebee.models.Item;
import com.chargebee.models.ItemPrice;
import com.chargebee.models.Order;

import jakarta.transaction.Transactional;

@Service
public class ChargebeeSyncService {
    
    @Autowired
    private com.bookmyjuice.repository.SubscriptionEntityRepository subscriptionEntityRepository;
    @Autowired
    private com.bookmyjuice.repository.SubscriptionItemEntityRepository subscriptionItemEntityRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChargebeeSyncService.class);
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private ItemPriceRepository itemPriceRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ItemService itemService;
    
    @Autowired
    private ItemPriceService itemPriceService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private com.bookmyjuice.config.ChargebeeSyncConfig syncConfig;
    
    private ExecutorService executorService;
    
    /**
     * This method runs automatically when the application starts up
     * It ensures all Chargebee data is synchronized with the local database
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncChargebeeDataOnStartup() {
        // Check if startup sync is enabled
        if (!syncConfig.isEnableStartupSync()) {
            logger.info("📛 Chargebee startup sync is disabled. Skipping synchronization.");
            return;
        }
        
        logger.info("🚀 Starting Chargebee data synchronization on application startup...");
        
        // Initialize thread pool with configured size
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(syncConfig.getThreadPoolSize());
        }
        try {
            // Run synchronization tasks in parallel for better performance
            CompletableFuture<Void> itemsSync = CompletableFuture.runAsync(this::syncItems, executorService);
            CompletableFuture<Void> customersSync = CompletableFuture.runAsync(this::syncCustomers, executorService);
            CompletableFuture<Void> ordersSync = CompletableFuture.runAsync(this::syncOrders, executorService);
            
            
            // Wait for items to complete first, then sync item prices (they depend on items)
            itemsSync.get();
            CompletableFuture<Void> itemPricesSync = CompletableFuture.runAsync(this::syncItemPrices, executorService);
            
            CompletableFuture<Void> subscriptionsSync = CompletableFuture.runAsync(this::syncSubscriptions, executorService);
            // Wait for all sync operations to complete
            CompletableFuture.allOf(customersSync, itemPricesSync, ordersSync, subscriptionsSync).get();
            
            logger.info("✅ Chargebee data synchronization completed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Error during Chargebee data synchronization: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Synchronize all items from Chargebee
     */
    @Transactional
    private void syncItems() {
        logger.info("🔄 Synchronizing Items from Chargebee...");
        
        try {
            int totalItems = 0;
            int newItems = 0;
            int updatedItems = 0;
            
            // Fetch all items from Chargebee with pagination
            ListResult itemResults = Item.list()
                    .limit(syncConfig.getBatchSize())  // Use configured batch size
                    .request();
            
            do {
                logger.info("Processing {} items from Chargebee", itemResults.size());
                
                for (ListResult.Entry entry : itemResults) {
                    Item chargebeeItem = entry.item();
                    totalItems++;
                    
                    try {
                        // Check if item already exists in database
                        boolean exists = itemRepository.existsById(chargebeeItem.id());
                        
                        if (!exists) {
                            // Create new item entity
                            ItemEntity itemEntity = ItemMapper.toEntity(chargebeeItem);
                            itemRepository.save(itemEntity);
                            newItems++;
                            logger.debug("✅ Added new item: {} ({})", chargebeeItem.name(), chargebeeItem.id());
                        } else {
                            // Update existing item
                            ItemEntity existingItem = itemRepository.findById(chargebeeItem.id()).orElse(null);
                            if (existingItem != null) {
                                ItemEntity updatedEntity = ItemMapper.toEntity(chargebeeItem);
                                updatedEntity.setItemPrices(existingItem.getItemPrices()); // Preserve existing relationships
                                itemRepository.save(updatedEntity);
                                updatedItems++;
                                logger.debug("🔄 Updated existing item: {} ({})", chargebeeItem.name(), chargebeeItem.id());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.error("❌ Error processing item {}: {}", chargebeeItem.id(), e.getMessage());
                    }
                }
                
                // Move to next page if available
                if (itemResults.nextOffset() != null) {
                    itemResults = Item.list()
                            .limit(syncConfig.getBatchSize())
                            .offset(itemResults.nextOffset())
                            .request();
                } else {
                    break;
                }
                
            } while (true);
            
            logger.info("📊 Items sync completed - Total: {}, New: {}, Updated: {}", totalItems, newItems, updatedItems);
            
        } catch (Exception e) {
            logger.error("❌ Error syncing items from Chargebee: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Synchronize all item prices from Chargebee and link them to items
     */
    @Transactional
    private void syncItemPrices() {
        logger.info("🔄 Synchronizing Item Prices from Chargebee...");
        
        try {
            int totalItemPrices = 0;
            int newItemPrices = 0;
            int updatedItemPrices = 0;
            int orphanedItemPrices = 0;
            
            // Fetch all item prices from Chargebee with pagination
            ListResult itemPriceResults = ItemPrice.list()
                    .limit(syncConfig.getBatchSize())
                    .request();
            
            do {
                logger.info("Processing {} item prices from Chargebee", itemPriceResults.size());
                
                for (ListResult.Entry entry : itemPriceResults) {
                    ItemPrice chargebeeItemPrice = entry.itemPrice();
                    totalItemPrices++;
                    
                    try {
                        // Check if item price already exists in database
                        boolean exists = itemPriceRepository.existsById(chargebeeItemPrice.id());
                        
                        // Find the parent item for this item price
                        ItemEntity parentItem = itemRepository.findById(chargebeeItemPrice.itemId()).orElse(null);
                        
                        if (parentItem == null) {
                            logger.warn("⚠️ Parent item not found for ItemPrice: {} (Item ID: {})", 
                                    chargebeeItemPrice.id(), chargebeeItemPrice.itemId());
                            orphanedItemPrices++;
                            continue;
                        }
                        
                        if (!exists) {
                            // Create new item price entity with proper parent-child relationship
                            ItemPriceEntity itemPriceEntity = mapItemPriceToEntity(chargebeeItemPrice, parentItem);
                            itemPriceRepository.save(itemPriceEntity);
                            
                            // Add to parent item's collection
                            parentItem.addItemPrice(itemPriceEntity);
                            itemRepository.save(parentItem);
                            
                            newItemPrices++;
                            logger.debug("✅ Added new item price: {} linked to item: {}", 
                                    chargebeeItemPrice.id(), parentItem.getName());
                        } else {
                            // Update existing item price
                            ItemPriceEntity existingItemPrice = itemPriceRepository.findById(chargebeeItemPrice.id()).orElse(null);
                            if (existingItemPrice != null) {
                                ItemPriceEntity updatedEntity = mapItemPriceToEntity(chargebeeItemPrice, parentItem);
                                updatedEntity.setItem(existingItemPrice.getItem()); // Preserve existing relationship
                                itemPriceRepository.save(updatedEntity);
                                updatedItemPrices++;
                                logger.debug("🔄 Updated existing item price: {}", chargebeeItemPrice.id());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.error("❌ Error processing item price {}: {}", chargebeeItemPrice.id(), e.getMessage());
                    }
                }
                
                // Move to next page if available
                if (itemPriceResults.nextOffset() != null) {
                    itemPriceResults = ItemPrice.list()
                            .limit(syncConfig.getBatchSize())
                            .offset(itemPriceResults.nextOffset())
                            .request();
                } else {
                    break;
                }
                
            } while (true);
            
            logger.info("📊 Item Prices sync completed - Total: {}, New: {}, Updated: {}, Orphaned: {}", 
                    totalItemPrices, newItemPrices, updatedItemPrices, orphanedItemPrices);
            
        } catch (Exception e) {
            logger.error("❌ Error syncing item prices from Chargebee: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Synchronize all customers from Chargebee
     */
    @Transactional
    private void syncCustomers() {
        logger.info("🔄 Synchronizing Customers from Chargebee...");
        
        try {
            int totalCustomers = 0;
            int newCustomers = 0;
            int updatedCustomers = 0;
            
            // Fetch all customers from Chargebee with pagination
            ListResult customerResults = Customer.list()
                    .limit(syncConfig.getBatchSize())
                    .request();
            
            do {
                logger.info("Processing {} customers from Chargebee", customerResults.size());
                
                for (ListResult.Entry entry : customerResults) {
                    Customer chargebeeCustomer = entry.customer();
                    totalCustomers++;
                    
                    try {
                        // Check if customer already exists in database
                        boolean exists = customerRepository.existsById(chargebeeCustomer.id());
                        
                        if (!exists) {
                            // Create new customer entity
                            CustomerEntity customerEntity = CustomerMapper.toEntity(chargebeeCustomer);
                            customerRepository.save(customerEntity);
                            newCustomers++;
                            logger.debug("✅ Added new customer: {} ({})", 
                                    chargebeeCustomer.email(), chargebeeCustomer.id());
                        } else {
                            // Update existing customer
                            CustomerEntity existingCustomer = customerRepository.findById(chargebeeCustomer.id()).orElse(null);
                            if (existingCustomer != null) {
                                CustomerEntity updatedEntity = CustomerMapper.toEntity(chargebeeCustomer);
                                updatedEntity.setSubscriptions(existingCustomer.getSubscriptions()); // Preserve relationships
                                customerRepository.save(updatedEntity);
                                updatedCustomers++;
                                logger.debug("🔄 Updated existing customer: {} ({})", 
                                        chargebeeCustomer.email(), chargebeeCustomer.id());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.error("❌ Error processing customer {}: {}", chargebeeCustomer.id(), e.getMessage());
                    }
                }
                
                // Move to next page if available
                if (customerResults.nextOffset() != null) {
                    customerResults = Customer.list()
                            .limit(syncConfig.getBatchSize())
                            .offset(customerResults.nextOffset())
                            .request();
                } else {
                    break;
                }
                
            } while (true);
            
            logger.info("📊 Customers sync completed - Total: {}, New: {}, Updated: {}", 
                    totalCustomers, newCustomers, updatedCustomers);
            
        } catch (Exception e) {
            logger.error("❌ Error syncing customers from Chargebee: {}", e.getMessage(), e);
        }
    }
        /**
     * Synchronize all subscriptions and their items from Chargebee
     */
    @Transactional
    private void syncSubscriptions() {
        logger.info("🔄 Synchronizing Subscriptions from Chargebee...");
        try {
            int totalSubscriptions = 0, newSubscriptions = 0, updatedSubscriptions = 0;
            com.chargebee.ListResult subscriptionResults = com.chargebee.models.Subscription.list().limit(syncConfig.getBatchSize()).request();
            do {
                logger.info("Processing {} subscriptions from Chargebee", subscriptionResults.size());
                for (com.chargebee.ListResult.Entry entry : subscriptionResults) {
                    com.chargebee.models.Subscription chargebeeSubscription = entry.subscription();
                    totalSubscriptions++;
                    try {
                        boolean exists = subscriptionEntityRepository.existsById(chargebeeSubscription.id());
                        if (!exists) {
                            // Map and save new subscription
                            com.bookmyjuice.models.entities.SubscriptionEntity subscriptionEntity = com.bookmyjuice.models.mappers.SubscriptionMapper.toEntity(chargebeeSubscription);
                            // Set parent for each item
                                if (subscriptionEntity.getSubscriptionItems() != null) {
                                for (com.bookmyjuice.models.entities.SubscriptionItemEntity item : subscriptionEntity.getSubscriptionItems()) {
                                    item.setSubscription(subscriptionEntity);
                                }
                            }
                            subscriptionEntityRepository.save(subscriptionEntity);
                            newSubscriptions++;
                            logger.debug("✅ Added new subscription: {}", chargebeeSubscription.id());
                        } else {
                            // Update existing subscription
                            com.bookmyjuice.models.entities.SubscriptionEntity existing = subscriptionEntityRepository.findById(chargebeeSubscription.id()).orElse(null);
                            if (existing != null) {
                                com.bookmyjuice.models.entities.SubscriptionEntity updated = com.bookmyjuice.models.mappers.SubscriptionMapper.toEntity(chargebeeSubscription);
                                // Preserve relationships
                                updated.setCustomer(existing.getCustomer());
                                updated.setShippingAddress(existing.getShippingAddress());
                                // Set parent for each item
                                if (updated.getSubscriptionItems() != null) {
                                    for (com.bookmyjuice.models.entities.SubscriptionItemEntity item : updated.getSubscriptionItems()) {
                                        item.setSubscription(updated);
                                    }
                                }
                                subscriptionEntityRepository.save(updated);
                                updatedSubscriptions++;
                                logger.debug("🔄 Updated subscription: {}", chargebeeSubscription.id());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("❌ Error processing subscription {}: {}", chargebeeSubscription.id(), e.getMessage());
                    }
                }
                // Move to next page if available
                if (subscriptionResults.nextOffset() != null) {
                    subscriptionResults = com.chargebee.models.Subscription.list()
                        .limit(syncConfig.getBatchSize())
                        .offset(subscriptionResults.nextOffset())
                        .request();
                } else {
                    break;
                }
            } while (true);
            logger.info("📊 Subscriptions sync completed - Total: {}, New: {}, Updated: {}", totalSubscriptions, newSubscriptions, updatedSubscriptions);
        } catch (Exception e) {
            logger.error("❌ Error syncing subscriptions from Chargebee: {}", e.getMessage(), e);
        }
    }
    /**
     * Synchronize all orders from Chargebee
     */
    @Transactional
    private void syncOrders() {
        logger.info("🔄 Synchronizing Orders from Chargebee...");
        try {
            int totalOrders = 0, newOrders = 0, updatedOrders = 0;
            // Fetch all orders from Chargebee with pagination
            ListResult orderResults = Order.list().limit(syncConfig.getBatchSize()).request();
            do {
                logger.info("Processing {} orders from Chargebee", orderResults.size());
                
                for (ListResult.Entry entry : orderResults) {
                    Order chargebeeOrder = entry.order();
                    totalOrders++;
                    try {
                        // Check if order already exists in database
                        boolean exists = orderRepository.existsById(chargebeeOrder.id());
                        
                        if (!exists) {
                            // Create new order entity
                            OrderEntity orderEntity = OrderMapper.toEntity(chargebeeOrder);
                            orderRepository.save(orderEntity);
                            newOrders++;
                            logger.debug("✅ Added new order: {} ({})", chargebeeOrder.id(), chargebeeOrder.customerId());
                        } else {
                            // Update existing order
                            OrderEntity existingOrder = orderRepository.findById(chargebeeOrder.id()).orElse(null);
                            if (existingOrder != null) {
                                OrderEntity updatedEntity = OrderMapper.toEntity(chargebeeOrder);
                                orderRepository.save(updatedEntity);
                                updatedOrders++;
                                logger.debug("🔄 Updated existing order: {} ({})", chargebeeOrder.id(), chargebeeOrder.customerId());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.error("❌ Error processing order {}: {}", chargebeeOrder.id(), e.getMessage());
                    }
                }
                
                // Move to next page if available
                if (orderResults.nextOffset() != null) {
                    orderResults = Order.list()
                            .limit(syncConfig.getBatchSize())
                            .offset(orderResults.nextOffset())
                            .request();
                } else {
                    break;
                }
                
            } while (true);
            
            logger.info("📊 Orders sync completed - Total: {}, New: {}, Updated: {}", totalOrders, newOrders, updatedOrders);
            
        } catch (Exception e) {
            logger.error("❌ Error syncing orders from Chargebee: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Manual sync method that can be called programmatically
     */
    public void performManualSync() {
        logger.info("🔄 Starting manual Chargebee data synchronization...");
        
        // Initialize thread pool if not already done
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(syncConfig.getThreadPoolSize());
        }
        
        syncChargebeeDataOnStartup();
    }


    
    /**
     * Helper method to map Chargebee ItemPrice to ItemPriceEntity with proper relationships
     */
    private ItemPriceEntity mapItemPriceToEntity(ItemPrice chargebeeItemPrice, ItemEntity parentItem) {
        ItemPriceEntity entity = new ItemPriceEntity();
        
        entity.setId(chargebeeItemPrice.id());
        entity.setName(chargebeeItemPrice.name());
        entity.setExternalName(chargebeeItemPrice.externalName());
        entity.setDescription(chargebeeItemPrice.description());
        entity.setPricingModel(chargebeeItemPrice.pricingModel() != null ? chargebeeItemPrice.pricingModel().name() : null);
        entity.setPrice(chargebeeItemPrice.price() != null ? java.math.BigDecimal.valueOf(chargebeeItemPrice.price()) : null);
        entity.setCurrencyCode(chargebeeItemPrice.currencyCode());
        entity.setStatus(chargebeeItemPrice.status() != null ? chargebeeItemPrice.status().name() : null);
        entity.setPeriodUnit(chargebeeItemPrice.periodUnit() != null ? chargebeeItemPrice.periodUnit().name() : null);
        entity.setPeriod(chargebeeItemPrice.period());
        entity.setTrialAvailable(chargebeeItemPrice.trialPeriod() != null && chargebeeItemPrice.trialPeriod() > 0);
        entity.setTrialPeriod(chargebeeItemPrice.trialPeriod());
        entity.setTrialPeriodUnit(chargebeeItemPrice.trialPeriodUnit() != null ? chargebeeItemPrice.trialPeriodUnit().name() : null);
        entity.setFreeQuantityInDecimal(chargebeeItemPrice.freeQuantityInDecimal() != null ? 
            Boolean.TRUE : Boolean.FALSE);
        entity.setInvoiceNotes(chargebeeItemPrice.invoiceNotes());
        
        // Handle timestamps - Chargebee uses Timestamp objects
        if (chargebeeItemPrice.createdAt() != null) {
            entity.setCreatedAt(java.time.LocalDateTime.ofEpochSecond(chargebeeItemPrice.createdAt().getTime() / 1000, 0, java.time.ZoneOffset.UTC));
        }
        if (chargebeeItemPrice.updatedAt() != null) {
            entity.setUpdatedAt(java.time.LocalDateTime.ofEpochSecond(chargebeeItemPrice.updatedAt().getTime() / 1000, 0, java.time.ZoneOffset.UTC));
        }
        
        // Set the parent-child relationship
        entity.setItem(parentItem);
        
        // Handle metadata if present - using same pattern as existing code
        if (chargebeeItemPrice.metadata() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                entity.setMetadata(objectMapper.writeValueAsString(chargebeeItemPrice.metadata()));
            } catch (Exception e) {
                logger.warn("Failed to serialize metadata for ItemPrice {}: {}", chargebeeItemPrice.id(), e.getMessage());
            }
        }
        
        return entity;
    }
    
    /**
     * Method to get sync status/statistics
     */
    public String getSyncStatus() {
        long itemCount = itemRepository.count();
        long itemPriceCount = itemPriceRepository.count();
        long customerCount = customerRepository.count();
        
        return String.format("Database Status - Items: %d, ItemPrices: %d, Customers: %d", 
                itemCount, itemPriceCount, customerCount);
    }
    
    /**
     * Cleanup method to shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}