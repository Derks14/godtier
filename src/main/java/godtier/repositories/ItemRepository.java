package godtier.repositories;

import godtier.models.Item;
import godtier.models.Tier;
import godtier.models.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {
    Page<Item> findAllBy(TextCriteria criteria, Pageable pageable);

    void deleteAllByTopic(Topic topic);

    void deleteAllByTier(Tier tier);
}
