package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();
    private int processedCount = 0;


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */


    /**
     * Processes all items asynchronously.
     * Each item is fetched, status updated, and saved.
     * Thread-safe and returns only successfully processed items.
     */
    @Async
    public List<Item> processItemsAsync() {
        List<Long> ids = itemRepository.findAllIds();

        // Thread-safe collection for processed items
        ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();

        // Create a list of async tasks
        List<CompletableFuture<Void>> futures = ids.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        // Retrieve item and mark it as processed
                        Optional<Item> optItem = itemRepository.findById(id);
                        optItem.ifPresent(item -> {
                            item.setStatus("PROCESSED");
                            itemRepository.save(item);
                            processedItems.add(item);
                        });
                    } catch (Exception e) {
                        // Log or handle errors if needed
                        System.err.println("Error processing item ID " + id + ": " + e.getMessage());
                    }
                }, executor))
                .collect(Collectors.toList());

        // Wait for all async operations to complete
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.get(); // blocks until all are finished
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error processing items", e);
        }

        return new ArrayList<>(processedItems);
    }

}

