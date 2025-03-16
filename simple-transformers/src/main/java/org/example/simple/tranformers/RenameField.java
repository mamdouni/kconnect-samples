package org.example.simple.tranformers;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public abstract class RenameField <R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String CURRENT_FIELD_CONFIG = "field.current";
    public static final String NEW_FIELD_CONFIG = "field.new";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(CURRENT_FIELD_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Field name to rename")
            .define(NEW_FIELD_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Field new name")
            ;
    // as you can see, this transformer can only work with string fields (don't use antoher type of field)

    protected abstract Schema operatingSchema(R kafkaRecord);

    protected abstract Object operatingValue(R kafkaRecord);

    protected abstract R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue);

    private String currentFieldName;
    private String newFieldName;

    @Override
    public void configure(Map<String, ?> map) {

        SimpleConfig config = new SimpleConfig(CONFIG_DEF, map);
        currentFieldName = config.getString(CURRENT_FIELD_CONFIG);
        newFieldName = config.getString(NEW_FIELD_CONFIG);

        log.info("Configuration: ");
        config.values().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.toList()).forEach(log::info);
        log.info("Current Field Name: {}", currentFieldName);
        log.info("New Field Name: {}", newFieldName);
    }

    @Override
    public R apply(R record) {

        if(record.value() == null) {
            return record;
        }
        if(operatingSchema(record) == null) {
            return insertIntegrityWithoutSchema(record);
        }else {
            return insertIntegrityWithSchema(record);
        }
    }

    private static final String PURPOSE = "rename a field";

    private R insertIntegrityWithoutSchema(R record) {

        // extract the value of the record which is a map
        Map<String,Object> recordValues = Requirements.requireMapOrNull(operatingValue(record), PURPOSE);

        Object currentValue = recordValues.get(currentFieldName);

        // create a new map with the updated value
        Map<String, Object> updatedRecordValues = new HashMap<>(recordValues);
        updatedRecordValues.put(newFieldName,currentValue);

        return newRecord(record, null, updatedRecordValues);
    }

    private R insertIntegrityWithSchema(R record) {

        //extracting the schema of the record
        Schema schema = operatingSchema(record);

        // copy the schema of the record ( name, version, comments, etc )
        SchemaBuilder builder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());

        // copy all the existing fields from old schema
        log.info("Current Schema Builder {}", builder);
        log.info("Current Schema fields: {}", schema.fields());
        for (Field field : schema.fields()) {

            // remove the current field name from the schema
            if(Objects.equals(field.name(), currentFieldName)) {
                continue;
            }
            builder.field(field.name(), field.schema());
        }

        // add one more field to our schema
        Schema updatedSchema = builder.field(newFieldName, Schema.STRING_SCHEMA).build();

        log.info("Updated Schema Builder {}", builder);
        log.info("Updated Schema Fields {}", builder.schema().fields());

        // extract the value of the record which is a struct and not a map anymore
        Struct recordValues = Requirements.requireStructOrNull(operatingValue(record), PURPOSE);
        log.info("Record Values {}", recordValues);

        Object currentValue = recordValues.get(currentFieldName);
        log.info("Current Value {}", currentValue);

        // create a new struct with the updated value
        Struct recordUpdatedValues = new Struct(updatedSchema);
        for (Field field : recordValues.schema().fields()) {

            // remove the current field name from the value
            if(Objects.equals(field.name(), currentFieldName)) {
                continue;
            }
            recordUpdatedValues.put(field.name(), recordValues.get(field));
        }
        recordUpdatedValues.put(newFieldName, currentValue);
        log.info("Record Updated Values {}", recordUpdatedValues);

        return newRecord(record, updatedSchema, recordUpdatedValues);
    }

    @Override
    public void close() {
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class Key<R extends ConnectRecord<R>> extends RenameField<R> {

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

    public static class Value<R extends ConnectRecord<R>> extends RenameField<R> {

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
