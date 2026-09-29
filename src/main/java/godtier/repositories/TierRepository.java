package godtier.repositories;

import godtier.models.Tier;
import godtier.models.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TierRepository extends MongoRepository<Tier, String> {
    Page<Tier> findAllBy(TextCriteria criteria, Pageable pageable);

    void deleteAllByTopic(Topic topic);
}
