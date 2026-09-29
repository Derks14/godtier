package godtier.services;

import godtier.config.GodtierException;
import godtier.dto.FetchRequest;
import godtier.dto.PaginationMapper;
import godtier.dto.PaginationMeta;
import godtier.dto.Res;
import godtier.models.Settings;
import godtier.models.Tier;
import godtier.models.Topic;
import godtier.repositories.ItemRepository;
import godtier.repositories.SettingsRepository;
import godtier.repositories.TierRepository;
import godtier.repositories.TopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class TopicService {
    long min = 1_000_000L;  // 1 Million
    long max = 100_000_000L;

    private final TopicRepository repository;
    private final MongoTemplate mongoTemplate;
    private final SettingsRepository settingsRepository;
    private final TierRepository tierRepository;
    private final ItemRepository itemRepository;

    public TopicService(TopicRepository repository, MongoTemplate mongoTemplate, SettingsRepository settingsRepository, TierRepository tierRepository, ItemRepository itemRepository) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.settingsRepository = settingsRepository;
        this.tierRepository = tierRepository;
        this.itemRepository = itemRepository;
    }

    public Res<List<Topic>> fetchTopics(FetchRequest request) {
        log.info("processing request to fetch details");
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        Query query = new Query();

        try {
            if (StringUtils.hasText(request.getSearch())) {
                TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(request.getSearch());
                TextQuery textQuery = TextQuery.queryText(criteria);

                textQuery.sortByScore();
                query = textQuery;
            }


            query.with(pageRequest);

            // we run our query with mongoTemplate via mongo
            List<Topic> results = mongoTemplate.find(query, Topic.class);

//            Query countQuery = Query.of(query).limit(-1).skip(-1);

//            this gives us topics via our code
            Query finalQuery = query;
            Page<Topic> topicsInPages = PageableExecutionUtils.getPage(
                    results,
                    pageRequest,
                    () -> mongoTemplate.count(Query.of(finalQuery).limit(-1).skip(-1), Topic.class));

            PaginationMeta paginationMeta = PaginationMapper.from(topicsInPages);
            List<Topic> topics = topicsInPages.getContent();

            log.info("topics fetched successfully: pagination {}. data:{}", paginationMeta, topics);

            return Res.<List<Topic>>builder()
                    .message("topics fetch successfully")
                    .data(results)
                    .pagination(paginationMeta)
                    .build();

        } catch (Exception e) {
            throw new GodtierException("fetch from store error",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getLocalizedMessage(),
                    e.getCause()
            );
        }

    }

    public Topic getTopic(String id) {
        log.info("processing request to fetch single topic");
        Topic topic = repository.findById(id).orElseThrow(
                () -> new GodtierException(
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        HttpStatus.NOT_FOUND,
                        "topic with id [%s] could not be found".formatted(id)
                ));

        log.info("topic with title: {}. found successfully", topic.getTitle());
        return topic;

    }


    @Transactional
    public Topic addTopic(String title) {
        log.info("processing request to add new topic");


        Topic topic = new Topic(title);
        Topic newTopic = repository.insert(topic);
        log.info("topic {}, created successfully", topic.getTitle());

        Settings settings = Settings.builder().topic(newTopic).build();
        Settings newSettings = settingsRepository.insert(settings);
        log.info("default settings for topic: {}, created and saved", newTopic.getTitle());


        Tier sTier = Tier.builder().title("s").topic(newTopic).order(BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(min, max))).build();
        Tier aTier = Tier.builder().title("a").topic(newTopic).order(BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(min, max))).build();
        Tier bTier = Tier.builder().title("b").topic(newTopic).order(BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(min, max))).build();

        List<Tier> tiers = List.of(sTier, aTier, bTier);
        tierRepository.insert(tiers);

        log.info("default tiers for topic: {}, created and saved", newTopic.getTitle());

        log.info("topic {} created successfully", title);
        return newTopic;
    }



    public Topic updateTopic(Topic newData, String id) {
        log.info("processing request to find update topic");

        Topic topic = repository.findById(id).orElseThrow(
                () -> new GodtierException(
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        HttpStatus.NOT_FOUND,
                        "topic with id [%s] could not be found".formatted(id)
                        )
        );

        try {
            topic.setTitle(newData.getTitle());
            repository.save(topic);
        }
        catch (Exception e) {
            throw new GodtierException(
                    "topic_update_failed",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getLocalizedMessage(),
                    e.getCause()
            );
        }

        log.info("topic, {}, has been successfully updated", topic.getId());
        return topic;
    }

    public Res deleteTopic(String id) {
        log.info("processing request to delete topic");

        Topic topic = repository.findById(id)
                .orElseThrow(() -> new GodtierException(
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        HttpStatus.NOT_FOUND,
                        "topic with id: %s, could not be found".formatted(id)
                ));

        // this should delete all tiers with topic
        tierRepository.deleteAllByTopic(topic);
        log.info("topic - {}, all tiers related to topic deleted", topic.getTitle());


        itemRepository.deleteAllByTopic(topic);
        log.info("topic - {}, all items related to topic deleted", topic.getTitle());


        repository.delete(topic);
        log.info("topic - {}, deleted successfully", topic.getTitle());
        return Res.builder().message("topic deleted successfully").build();
    }
}
