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
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class Identity<R extends ConnectRecord<R>> implements Transformation<R> {

    /*
        This is a simple class that is used to transform the key or value of a record into the same key or value.
        It is just an example to show the boilerplate code that is needed to create a transformation.
     */

    public static final ConfigDef CONFIG_DEF = new ConfigDef();

    protected abstract Schema operatingSchema(R kafkaRecord);

    protected abstract Object operatingValue(R kafkaRecord);

    protected abstract R newRecord(R kafkaRecord, Schema updatedSchema, Object updatedValue);

    @Override
    public void configure(Map<String, ?> map) {
    }

    @Override
    public R apply(R record) {

        log.info("Applying identity transformation ...");
        if(record.value() == null) {
            return record;
        }
        if(operatingSchema(record) == null) {
            return insertIntegrityWithoutSchema(record);
        }else {
            return insertIntegrityWithSchema(record);
        }
    }

    private static final String PURPOSE = "identity transformation";

    private R insertIntegrityWithoutSchema(R record) {

        // extract the value of the record which is a map
        Map<String,Object> recordValues = Requirements.requireMapOrNull(operatingValue(record), PURPOSE);

        // create a new map with the updated value
        Map<String, Object> updatedRecordValues = new HashMap<>(recordValues);

        return newRecord(record, null, updatedRecordValues);
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
        Schema updatedSchema = builder.build();

        // extract the value of the record which is a struct and not a map anymore
        Struct recordValues = Requirements.requireStructOrNull(operatingValue(record), PURPOSE);

        // create a new struct with the updated value
        Struct recordUpdatedValues = new Struct(updatedSchema);
        for (Field field : recordValues.schema().fields()) {
            recordUpdatedValues.put(field.name(), recordValues.get(field));
        }

        return newRecord(record, updatedSchema, recordUpdatedValues);
    }


    @Override
    public void close() {
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class Key<R extends ConnectRecord<R>> extends Identity<R> {

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

    public static class Value<R extends ConnectRecord<R>> extends Identity<R> {

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
