package godtier.controllers;

import godtier.dto.FetchRequest;
import godtier.dto.Res;
import godtier.models.Item;
import godtier.models.Topic;
import godtier.services.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/item")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<Res<List<Item>>> fetchItems(
            @RequestParam String topicId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request
    ) {
        log.info("new request to fetch topics");

        FetchRequest fetchRequest = FetchRequest.builder()
                .page(page)
                .size(size)
                .search(Strings.trimToNull(search))
                .build();

        Res<List<Item>> response = itemService.fetchItems(fetchRequest, topicId);

        response.setTimestamp(Instant.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("{id}")
    public ResponseEntity<Res<Item>> fetchItem(@PathVariable String id,
                                               HttpServletRequest request) {
        log.info("request to fetch new item");
        Item item = itemService.getItem(id);
        Res<Item> response = new Res<>(
                "single item fetched successfully",
                item ,
                request.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Res<Item>> createItem(@RequestBody Item item,
                                                @RequestParam String topicId,
                                                HttpServletRequest request ) {
        log.info("request to create new item");
        Item createdItem = itemService.addItem(item, topicId);
        Res<Item> response = Res.<Item>builder()
                .message("item created successfully")
                .data(item)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("{id}")
    public ResponseEntity<Res<Item>> updateItem(@PathVariable String id,
                                                @RequestBody Item item,
                                                HttpServletRequest request
                                                ) {
        log.info("new request to update item");
        Item updatedItem = itemService.updateItem(item, id);

        Res<Item> response = Res.<Item>builder()
                .message("item updated successfully")
                .data(item)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }
}
