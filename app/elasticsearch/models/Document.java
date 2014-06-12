package elasticsearch.models;


import org.springframework.data.annotation.Id;

import java.io.Serializable;

@org.springframework.data.elasticsearch.annotations.Document(indexName = "documentsearch", type = "document")
public class Document implements Serializable {

    @Id
    public String id;
    public String content;
    public String name;
    public String folder;
}
