package org.example.json.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.Requirements;
import org.apache.kafka.connect.transforms.util.SchemaUtil;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.example.json.transformers.converters.PurchaseItemConverter;
import org.example.json.transformers.schemas.PurchaseItemSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class PurchaseItemToArray<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String JSON_FIELD_CONFIG = "field";
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(JSON_FIELD_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Json field name to format");

    protected abstract Schema operatingSchema(R kafkaRecord);

    protected abstract Object operatingValue(R kafkaRecord);

    protected abstract R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue);

    private String fieldName;

    @Override
    public void configure(Map<String, ?> map) {

        SimpleConfig config = new SimpleConfig(CONFIG_DEF, map);
        fieldName = config.getString(JSON_FIELD_CONFIG);
    }

    @Override
    public R apply(R record) {

        try {
            log.info("Playing PurchaseToArray transformation ...");
            if(record.value() == null) {
                return record;
            }
            if(operatingSchema(record) == null) {
                return formatWithoutSchema(record);
            }else {
                return formatWithSchema(record);
            }
        } catch (JsonProcessingException e) {
            log.error("Error during parsing on the PurchaseToArray transformer", e);
            return record;
        }
    }

    private static final String PURPOSE = "format json object";

    private R formatWithoutSchema(R record) throws JsonProcessingException {

        Map<String,Object> recordValues = Requirements.requireMapOrNull(operatingValue(record), PURPOSE);
        Map<String, Object> updatedRecordValues = new HashMap<>(recordValues);
        updatedRecordValues.put(this.fieldName, convertToStructArray((String) recordValues.get(this.fieldName)));
        return newRecord(record, null, updatedRecordValues);
    }

    private R formatWithSchema(R record) throws JsonProcessingException {

        // schema
        Schema schema = operatingSchema(record);
        SchemaBuilder builder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());
        for (Field field : schema.fields()) {

            if (field.name().equals(this.fieldName)) {
                builder.field(field.name(), PurchaseItemSchema.PURCHASE_ITEM_ARRAY_SCHEMA_FLAT);
            } else {
                builder.field(field.name(), field.schema());
            }
        }
        Schema updatedSchema = builder.build();

        // values
        Struct recordValues = Requirements.requireStructOrNull(operatingValue(record), PURPOSE);
        Struct recordUpdatedValues = new Struct(updatedSchema);
        for (Field field : recordValues.schema().fields()) {

            if(Objects.equals(field.name(), this.fieldName)) {
                continue;
            }
            recordUpdatedValues.put(field.name(), recordValues.get(field));
        }
        recordUpdatedValues.put(this.fieldName, convertToStructArray((String) recordValues.get(this.fieldName)));

        return newRecord(record, updatedSchema, recordUpdatedValues);
    }

    private List<Struct> convertToStructArray(String value) throws JsonProcessingException {
        return PurchaseItemConverter.convertToStructArray(value);
    }


    @Override
    public void close() {
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class Key<R extends ConnectRecord<R>> extends PurchaseItemToArray<R> {

        @Override
        protected Schema operatingSchema(R kafkaRecord) {
            return kafkaRecord.keySchema();
        }

        @Override
        protected Object operatingValue(R kafkaRecord) {
            return kafkaRecord.key();
        }

        @Override
        protected R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue) {
            return kafkaRecord.newRecord(kafkaRecord.topic(), kafkaRecord.kafkaPartition(), updatedSchema, updatedValue, kafkaRecord.valueSchema(), kafkaRecord.value(), kafkaRecord.timestamp());
        }

    }

    public static class Value<R extends ConnectRecord<R>> extends PurchaseItemToArray<R> {

        @Override
        protected Schema operatingSchema(R kafkaRecord) {
            return kafkaRecord.valueSchema();
        }

        @Override
        protected Object operatingValue(R kafkaRecord) {
            return kafkaRecord.value();
        }

        @Override
        protected R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue) {
            return kafkaRecord.newRecord(kafkaRecord.topic(), kafkaRecord.kafkaPartition(), kafkaRecord.keySchema(), kafkaRecord.key(), updatedSchema, updatedValue, kafkaRecord.timestamp());
        }
    }
}
