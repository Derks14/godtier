package godtier.controllers;

import godtier.dto.FetchRequest;
import godtier.dto.Res;
import godtier.models.Topic;
import godtier.services.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<Res<List<Topic>>> fetch(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request
    ) {
        log.info("new request to fetch details");

        FetchRequest fetchRequest = FetchRequest.builder()
                .page(page)
                .size(size)
                .search(Strings.trimToNull(search))
                .build();

        Res<List<Topic>> response = topicService.fetchTopics(fetchRequest);
        response.setTimestamp(Instant.now());
        return ResponseEntity.ok(response);
    }


    @GetMapping("{id}")
    public ResponseEntity<Res<Topic>> fetchTopic(@PathVariable String id,
                                                 HttpServletRequest request) {
        log.info("new request to fetch single topic");
        Topic topic = topicService.getTopic(id);
        Res<Topic> response = new Res<>("single topic fetched successfully", topic, request.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Res<Topic>> createTopic(@RequestBody Topic topic, HttpServletRequest request) {
        log.info("new request to add new topic");

        Topic topic1 = topicService.addTopic(topic.getTitle());
        Res<Topic> response = new Res<>("new topic created successfully", topic1, request.getRequestURI());
        return ResponseEntity.ok(response);
    }

    @PutMapping("{id}")
    public ResponseEntity<Res<Topic>> updateTopic(
            @RequestBody Topic topic,
            @PathVariable String id,
            HttpServletRequest request
    ) {
        log.info("new request to update topic");

        Topic  updatedTopic = this.topicService.updateTopic(topic, id);

        Res<Topic> response = Res.<Topic>builder()
                .message("topic updated successfully")
                .data(updatedTopic)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("{id}")
    public ResponseEntity<Res> deleteTopic(@PathVariable String id, HttpServletRequest request) {
        log.info("request to delete topic");
        Res response = topicService.deleteTopic(id);
        response.setPath(request.getRequestURI());
        return ResponseEntity.ok(response);
    }
}
