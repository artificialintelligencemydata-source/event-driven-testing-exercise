package com.acuver.autwit.adapter.mongo;

import com.mongodb.client.MongoCursor;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

@Component
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
public class MongoSchemaInitializer {

    private static final Logger log = LogManager.getLogger(MongoSchemaInitializer.class);

    // Box drawing
    private static final int CONTENT_WIDTH = 59;
    private static final String BORDER_LEFT = "│  ";
    private static final String BORDER_RIGHT = "  │";
    private static final String TOP_LINE =    "┌───────────────────────────────────────────────────────────────┐";
    private static final String BOTTOM_LINE = "└───────────────────────────────────────────────────────────────┘";
    private static final String MIDDLE_LINE = "├───────────────────────────────────────────────────────────────┤";

    @Autowired
    private MongoTemplate mongo;

    @PostConstruct
    public void initialize() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║            MONGODB SCHEMA INITIALIZATION                      ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        log.info(TOP_LINE);
        log.info(createBoxLine("COLLECTIONS & INDEXES", true));
        log.info(MIDDLE_LINE);

        // Event Context Collection
        createIfNotExists("event_context");
        ensureIndex("event_context", "canonicalKey", Sort.Direction.ASC, true);
        ensureIndex("event_context", "orderId", Sort.Direction.ASC, false);
        ensureIndex("event_context", "resumeReady", Sort.Direction.ASC, false);

        // Event Store Collection
        createIfNotExists("event_store");
        ensureIndex("event_store", "orderId", Sort.Direction.ASC, false);
        ensureIndex("event_store", "eventType", Sort.Direction.ASC, false);

        // Scenario Context Collection
        createIfNotExists("scenario_context");
        ensureIndex("scenario_context", "scenarioName", Sort.Direction.ASC, false);
        ensureIndex("scenario_context", "lastUpdated", Sort.Direction.ASC, false);

        log.info(BOTTOM_LINE);

        // Print summary
        printSummary();

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║            [OK] MONGODB SCHEMA INITIALIZED                    ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    private void createIfNotExists(String name) {
        if (!mongo.collectionExists(name)) {
            mongo.createCollection(name);
            log.info(createBoxLine("[CREATED] Collection: " + name, false));
        } else {
            log.info(createBoxLine("[EXISTS] Collection: " + name, false));
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
            Bson indexKeys = direction == Direction.ASC
                    ? Indexes.ascending(field)
                    : Indexes.descending(field);

            IndexOptions options = new IndexOptions().unique(unique);
            String indexName = collection.createIndex(indexKeys, options);

            String uniqueStr = unique ? "UNIQUE" : "NON-UNIQUE";
            String dirStr = direction == Direction.ASC ? "ASC" : "DESC";
            log.info(createBoxLine("  [+] Index: " + field + " (" + uniqueStr + ", " + dirStr + ")", false));
        } else {
            log.debug("  [OK] Index exists: {} -> {}", collectionName, field);
        }
    }

    private void printSummary() {
        try {
            log.info(MIDDLE_LINE);
            log.info(TOP_LINE);
            log.info(createBoxLine("SCHEMA SUMMARY", true));
            log.info(MIDDLE_LINE);

            // Count collections
            int collectionCount = 0;
            for (String name : mongo.getCollectionNames()) {
                collectionCount++;
            }
            log.info(createKeyValueLine("Total Collections", String.valueOf(collectionCount)));

            // Check each collection
            checkCollection("event_context");
            checkCollection("event_store");
            checkCollection("scenario_context");

            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.warn("Could not generate summary: {}", e.getMessage());
        }
    }

    private void checkCollection(String collectionName) {
        try {
            if (mongo.collectionExists(collectionName)) {
                long count = mongo.getCollection(collectionName).countDocuments();
                MongoCollection<Document> collection = mongo.getCollection(collectionName);

                int indexCount = 0;
                try (MongoCursor<Document> cursor = collection.listIndexes().iterator()) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        indexCount++;
                    }
                }

                log.info(createBoxLine("[OK] " + collectionName, false));
                log.info(createBoxLine("     Documents: " + count + " | Indexes: " + indexCount, false));
            } else {
                log.info(createBoxLine("[NOT FOUND] " + collectionName, false));
            }
        } catch (Exception e) {
            log.debug("Could not check collection {}: {}", collectionName, e.getMessage());
        }
    }

    private String createBoxLine(String text, boolean centered) {
        if (text == null) {
            text = "";
        }

        if (text.length() > CONTENT_WIDTH) {
            text = text.substring(0, CONTENT_WIDTH);
        }

        StringBuilder line = new StringBuilder(BORDER_LEFT);

        if (centered) {
            int totalPadding = CONTENT_WIDTH - text.length();
            int leftPadding = totalPadding / 2;
            int rightPadding = totalPadding - leftPadding;

            for (int i = 0; i < leftPadding; i++) {
                line.append(' ');
            }
            line.append(text);
            for (int i = 0; i < rightPadding; i++) {
                line.append(' ');
            }
        } else {
            line.append(text);
            int padding = CONTENT_WIDTH - text.length();
            for (int i = 0; i < padding; i++) {
                line.append(' ');
            }
        }

        line.append(BORDER_RIGHT);
        return line.toString();
    }

    private String createKeyValueLine(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";

        String separator = " : ";
        int maxValueLength = CONTENT_WIDTH - key.length() - separator.length();

        if (value.length() > maxValueLength) {
            value = value.substring(0, maxValueLength);
        }

        StringBuilder line = new StringBuilder(BORDER_LEFT);
        line.append(key);
        line.append(separator);
        line.append(value);

        int currentLength = key.length() + separator.length() + value.length();
        int padding = CONTENT_WIDTH - currentLength;
        for (int i = 0; i < padding; i++) {
            line.append(' ');
        }

        line.append(BORDER_RIGHT);
        return line.toString();
    }
}