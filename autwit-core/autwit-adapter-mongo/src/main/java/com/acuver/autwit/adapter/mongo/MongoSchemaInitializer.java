package com.acuver.autwit.adapter.mongo;

import com.mongodb.client.MongoCursor;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.List;


@Component
public class MongoSchemaInitializer {

    private static final Logger log = LogManager.getLogger(MongoSchemaInitializer.class);
    @Autowired
    private MongoTemplate mongo;

    @PostConstruct
    public void initialize() {
        log.info("⏳ Initializing MongoDB schema...");

        createIfNotExists("event_context");
        ensureIndex("event_context", "canonicalKey", Sort.Direction.ASC, true);
        ensureIndex("event_context", "orderId", Sort.Direction.ASC, false);
        ensureIndex("event_context", "resumeReady", Sort.Direction.ASC, false);

        createIfNotExists("event_store");
        ensureIndex("event_store", "orderId", Sort.Direction.ASC, false);
        ensureIndex("event_store", "eventType", Sort.Direction.ASC, false);

        createIfNotExists("scenario_audit_log");
        ensureIndex("scenario_audit_log", "scenarioName", Sort.Direction.ASC, false);
        ensureIndex("scenario_audit_log", "lastUpdated", Sort.Direction.ASC, false);

        log.info("✅ MongoDB schema initialized.");
    }

    private void createIfNotExists(String name) {
        if (!mongo.collectionExists(name)) {
            mongo.createCollection(name);
            log.info("📁 Created collection: {}", name);
        }
    }

private void ensureIndex(String collectionName, String field, Direction direction, boolean unique) {
        MongoCollection<Document> collection = mongo.getCollection(collectionName);
        boolean exists = false;

        try (MongoCursor<Document> cursor = collection.listIndexes().iterator()) {
            while (cursor.hasNext()) {
                Document indexDoc = cursor.next();
                Document keys = (Document) indexDoc.get("key");
                if (keys != null && keys.containsKey(field)) {
                    boolean indexUnique = indexDoc.getBoolean("unique", false);
                    if (indexUnique == unique) {
                        exists = true;
                        break;
                    }
                }
            }
        }

        if (!exists) {
            // Indexes.ascending/descending returns Bson, not Document
            Bson indexKeys = direction == Direction.ASC
                    ? Indexes.ascending(field)
                    : Indexes.descending(field);

            IndexOptions options = new IndexOptions().unique(unique);
            String indexName = collection.createIndex(indexKeys, options);

            log.info("🆕 Created index '{}' on {} → {} ({})",
                    indexName, collectionName, field, direction);
        } else {
            log.debug("✔ Index exists on {} → {} ({})", collectionName, field, direction);
        }
    }
}