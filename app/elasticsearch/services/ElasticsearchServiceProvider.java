package elasticsearch.services;

import elasticsearch.repositories.DocumentRepository;
import elasticsearchplugin.ElasticsearchPlugin;
import elasticsearchplugin.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchServiceProvider extends ServiceProvider {

    @Autowired
    public DocumentRepository documentRepository;

    public static ElasticsearchServiceProvider get() {
        return ElasticsearchPlugin.get();
    }
}
