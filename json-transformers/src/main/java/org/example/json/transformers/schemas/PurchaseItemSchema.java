package org.example.json.transformers.schemas;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;

public final class PurchaseItemSchema {

    private PurchaseItemSchema() {
        throw new IllegalStateException("Cannot instantiate class");
    }

    public static final Schema PURCHASE_ITEM_SCHEMA_FLAT = SchemaBuilder.struct()
            .name("items")
            .field("item_id", Schema.INT32_SCHEMA)
            .field("name", Schema.STRING_SCHEMA)
            .field("price", Schema.INT32_SCHEMA)
            .build();

    public static final Schema PURCHASE_ITEM_ARRAY_SCHEMA_FLAT = SchemaBuilder.array(PURCHASE_ITEM_SCHEMA_FLAT).build();
}
