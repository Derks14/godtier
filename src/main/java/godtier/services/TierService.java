package godtier.services;

import godtier.config.GodtierException;
import godtier.dto.FetchRequest;
import godtier.dto.PaginationMapper;
import godtier.dto.PaginationMeta;
import godtier.dto.Res;
import godtier.models.Tier;
import godtier.models.Topic;
import godtier.repositories.ItemRepository;
import godtier.repositories.TierRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class TierService {
    private final TierRepository tierRepository;
    private final MongoTemplate mongoTemplate;
    private final TopicService topicService;
    private final ItemRepository itemRepository;

    public TierService(TierRepository tierRepository, MongoTemplate mongoTemplate, TopicService topicService, ItemRepository itemRepository) {
        this.tierRepository = tierRepository;
        this.mongoTemplate = mongoTemplate;
        this.topicService = topicService;
        this.itemRepository = itemRepository;
    }


    public Res<List<Tier>> fetchTiers(FetchRequest request, String topicId) {

        Topic topic = this.topicService.getTopic(topicId);


        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        Query query = new Query();
        try {
            if (StringUtils.hasText(request.getSearch())) {
                TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(request.getSearch());
                query = TextQuery.queryText(criteria);
                ((TextQuery) query).sortByScore();
            }
            query.with(pageRequest);


            if (!ObjectId.isValid(topicId)) throw new IllegalArgumentException("Invalid topic ID");


            query.addCriteria(Criteria.where("topic").is(new ObjectId(topicId)));

            List<Tier> results = mongoTemplate.find(query, Tier.class);

            Query finalQuery = query;
            Page<Tier> tiersInPages = PageableExecutionUtils.getPage(
                    results,
                    pageRequest,
                    () -> mongoTemplate.count(Query.of(finalQuery).limit(-1).skip(-1), Tier.class)
            );


            PaginationMeta paginationMeta = PaginationMapper.from(tiersInPages);
            List<Tier> tiers = tiersInPages.getContent();

            log.info("tiers fetch successfully. pagination {}. data: {}", paginationMeta, tiers);

            return Res.<List<Tier>>builder()
                    .message("tiers fetched successfully")
                    .data(results).pagination(paginationMeta)
                    .build();
        } catch (Exception e) {
            throw new GodtierException("fetch_from_store_error",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "process to fetch tiers from store failed"
                    );
        }
    }

    public Tier getTier(String id) {
        log.info("processing request to fetch single tier");

        Tier tier = tierRepository.findById(id)
                .orElseThrow(() -> new GodtierException(
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        HttpStatus.NOT_FOUND,
                        "tier with id [%s] could not found".formatted(id)
                ));

        log.info("tier with title {}. found", tier.getTitle());
        return tier;
    }

    public Tier createTier(Tier tier, String topicId) {
        try {
            log.info("processing request to add tier");

            Topic topic = topicService.getTopic(topicId);

            Tier newTier = Tier.builder()
                    .title(tier.getTitle())
                    .topic(topic)
                    .build();

            tierRepository.insert(newTier);
            log.info("tier {}, created successfully", tier.getTitle());
            return newTier;
        } catch (Exception e) {
            throw new GodtierException("failed_to_add_tier_list", HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e.getCause());
        }
    }

    public Tier updateTier(Tier tier, String id) {
        log.info("processing request to update tier");
        Tier tier1 = this.getTier(id);

        tier1.setTitle(tier.getTitle());
        tier1.setOrder(tier.getOrder());
        tier1.setColor(tier.getColor());

        tier1 = tierRepository.save(tier1);

        log.info("tier: {}, has been successfully updated", tier.getTitle());
        return tier1;
    }

    public Res deleteTier(String id) {
        log.info("processing request to delete tier");
        Tier tier = this.getTier(id);

        this.itemRepository.deleteAllByTier(tier);
        log.info("tier - {}, items related to tier deleted", tier.getTitle());

        this.tierRepository.delete(tier);
        log.info("tier - {}, deleted successfully", tier.getTitle());
        return Res.builder().message("tier deleted successfully").build();
    }



}
