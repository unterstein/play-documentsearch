package elasticsearch.repositories;

import elasticsearch.models.Document;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentRepository extends ElasticsearchRepository<Document, String> {

}
