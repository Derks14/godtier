package godtier.controllers;

import godtier.dto.FetchRequest;
import godtier.dto.Res;
import godtier.models.Tier;
import godtier.services.TierService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.parser.Entity;
import java.time.Instant;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/tiers")
public class TierController {

    private final TierService tierService;

    public TierController(TierService tierService) {
        this.tierService = tierService;
    }

    @GetMapping
    public ResponseEntity<Res<List<Tier>>> fetch(
            @RequestParam String topicId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request ) {
        log.info("new request to fetch tiers");
        FetchRequest fetchRequest = FetchRequest.builder()
                .page(page)
                .size(size)
                .search(Strings.trimToNull(search))
                .build();

        Res<List<Tier>> response = tierService.fetchTiers(fetchRequest, topicId);
        response.setTimestamp(Instant.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("{id}")
    public ResponseEntity<Res<Tier>> fetchTier(@PathVariable String id, HttpServletRequest request) {
        log.info("new request to fetch single tier");
        Tier tier = tierService.getTier(id);

        Res<Tier> response = Res.<Tier>builder()
                .message("tier retrieved successfully")
                .data(tier)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }


    @PutMapping("{id}")
    public ResponseEntity<Res<Tier>> updateTier(@PathVariable String id,
                                                @RequestBody Tier tier,
                                                HttpServletRequest request) {
        log.info("new request to update tier");

        Tier updatedTier = tierService.updateTier(tier, id);
        Res<Tier> response = Res.<Tier>builder()
                .message("tier retrieved successfully")
                .data(updatedTier)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Res<Tier>> createTier(@RequestBody Tier tier,
                                                @RequestParam String topicId,
                                                HttpServletRequest request) {
        log.info("new request to create tier");

        Tier createdTier = tierService.createTier(tier, topicId);

        Res<Tier> response = Res.<Tier>builder()
                .message("tier created successfully")
                .data(createdTier)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Res> deleteTier(@PathVariable String id, HttpServletRequest request) {
        log.info("new request to delete tier");
        Res response = tierService.deleteTier(id);

        return ResponseEntity.ok(response);
    }
}
