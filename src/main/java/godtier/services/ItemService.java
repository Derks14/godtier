package godtier.services;

import godtier.config.GodtierException;
import godtier.dto.FetchRequest;
import godtier.dto.PaginationMapper;
import godtier.dto.PaginationMeta;
import godtier.dto.Res;
import godtier.models.Item;
import godtier.models.Topic;
import godtier.repositories.ItemRepository;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ItemService {
    private final ItemRepository repository;
    private final TopicService topicService;
    private final MongoTemplate mongoTemplate;

    public ItemService(ItemRepository repository, TopicService topicService, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.topicService = topicService;
        this.mongoTemplate = mongoTemplate;
    }

    public Res<List<Item>> fetchItems(FetchRequest request, String topicId) {
        log.info("processing request to fetch details");

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        Query query;

        try {
            if (Objects.nonNull(request.getSearch())) {
                TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(request.getSearch());
                query = TextQuery.queryText(criteria);

                ((TextQuery) query).sortByScore();
            } else {
                query = new Query();
            }

            query.addCriteria(Criteria.where("topic").is(topicId));


            query.with(pageRequest);

            List<Item> results = mongoTemplate.find(query, Item.class);

            Page<Item> detailsInPages = PageableExecutionUtils.getPage(
                    results,
                    pageRequest,
                    () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Item.class));

            PaginationMeta paginationMeta = PaginationMapper.from(detailsInPages);
            List<Item> details = detailsInPages.getContent();

            log.info("details fetched successfully: pagination {}. data: {}", paginationMeta, details);

            return Res.<List<Item>>builder()
                    .message("details fetched successfully")
                    .data(results)
                    .pagination(paginationMeta)
                    .build();
        } catch (Exception e) {
            throw new GodtierException("fetch_from_store_error",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "process to fetch topics from store failed",
                    e.getCause());
        }
    }

    public Item getItem(String id) {
        log.info("processing request to fetch single item");

        Item item = repository.findById(id)
                .orElseThrow(()-> new GodtierException(
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        HttpStatus.NOT_FOUND,
                        "topic with id [%s] could not be found".formatted(id)
                ));
        log.info("item: {}. found successfully", item.getTitle());
        return item;
    }


    public Item addItem(Item item, String topicId) {
        log.info("processing request to add item");
        Topic topic = topicService.getTopic(topicId);
        item.setTopic(topic);

        repository.insert(item);
        return item;
    }


    public Item updateItem(Item item, String id) {
        log.info("processing request to update item");

        Item singleItem = this.getItem(id);

        try {
            singleItem.setTopic(item.getTopic());
            singleItem.setTier(item.getTier());
            singleItem.setTitle(item.getTitle());
            singleItem.setOrder(item.getOrder());
            singleItem.setDescription(item.getDescription());
            singleItem.setImageUrl(item.getImageUrl());

            repository.save(singleItem);
        } catch (Exception e) {
            throw new GodtierException("item_update_failed",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "item update failed",
                    e.getCause()
                    );
        }
        log.info("item: {}, has been successfully updated", item.getTitle());
        return singleItem;
    }

    public Res deleteItem(String id) {
        log.info("processing request to delete item");

        Item item = this.getItem(id);
        repository.delete(item);
        log.info("item deleted successfully");

        return Res.builder().message("item deleted successfully").build();
    }
}
