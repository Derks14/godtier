package godtier.models;


import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@EqualsAndHashCode(callSuper = true)
@CompoundIndex( name = "tier_order_idx", def = "{'tierId':1, 'order': 1}" )
public class Item extends BaseDocument {
    private String title;

    private String description;

    @DocumentReference
    private Tier tier;

    private int order;

    @DocumentReference
    private Topic topic;

    private String imageUrl;
}
