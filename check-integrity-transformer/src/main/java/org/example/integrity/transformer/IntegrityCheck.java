package org.example.integrity.transformer;

import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.stream.Collectors;

public abstract class IntegrityCheck<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String INTEGRITY_FIELD_CONFIG = "field";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(INTEGRITY_FIELD_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Integrity field name to add");

    protected abstract Schema operatingSchema(R kafkaRecord);

    protected abstract Object operatingValue(R kafkaRecord);

    protected abstract R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue);

    private String fieldName;

    // implementation from the Transformation interface
    @Override
    public void configure(Map<String, ?> map) {

        SimpleConfig config = new SimpleConfig(CONFIG_DEF, map);
        fieldName = config.getString(INTEGRITY_FIELD_CONFIG);
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

    private static final String PURPOSE = "integrity field addition";

    private R insertIntegrityWithoutSchema(R record) {

        // extract fields of our record as a map ( key is the field name and value is the field value )
        Map<String,Object> value = Requirements.requireMapOrNull(operatingValue(record), PURPOSE);
        String stringToHash = value.values()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining());
        String sha = DigestUtils.sha256Hex(stringToHash);

        // create a new map with the updated value
        Map<String, Object> updatedValue = new HashMap<>(value);
        updatedValue.put(fieldName,sha);

        return newRecord(record, null, updatedValue);
    }


    private R insertIntegrityWithSchema(R record) {

        //extracting the schema of the record
        Schema schema = operatingSchema(record);

        // copy the schema of the record ( name, version, comments, etc )
        SchemaBuilder builder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());

        // copy all the existing fields from old schema
        for (Field field : schema.fields()) {
            builder.field(field.name(), field.schema());
        }

        // add one more field to our schema
        Schema updatedSchema = builder.field(fieldName, Schema.STRING_SCHEMA).build();

        // extract the value of the record which is a struct and not a map anymore
        Struct value = Requirements.requireStructOrNull(operatingValue(record), PURPOSE);

        String stringToHash = value.schema().fields()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining());
        String sha = DigestUtils.sha256Hex(stringToHash);

        // create a new struct with the updated value
        Struct updateValue = new Struct(updatedSchema);
        for (Field field : value.schema().fields()) {
            updateValue.put(field.name(), value.get(field));
        }
        updateValue.put(fieldName, sha);

        return newRecord(record, updatedSchema, updateValue);
    }

    @Override
    public void close() {
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class Key<R extends ConnectRecord<R>> extends IntegrityCheck<R> {

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

    public static class Value<R extends ConnectRecord<R>> extends IntegrityCheck<R> {

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