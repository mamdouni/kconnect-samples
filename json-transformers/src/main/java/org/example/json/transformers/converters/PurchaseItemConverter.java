package org.example.json.transformers.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.example.json.transformers.dtos.ItemDTO;
import org.example.json.transformers.schemas.PurchaseItemSchema;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class PurchaseItemConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PurchaseItemConverter() {
        throw new IllegalStateException("Cannot instantiate class");
    }

    public static List<Struct> convertToStructArray(String value) throws JsonProcessingException {

        Arrays.stream(MAPPER.readValue(value, ItemDTO[].class)).forEach(e -> log.info("Mapped DTO : {}", e.toString()));
        return Arrays.stream(MAPPER.readValue(value, ItemDTO[].class))
                .map(item -> new Struct(PurchaseItemSchema.PURCHASE_ITEM_SCHEMA_FLAT)
                        .put("item_id", item.getItemId())
                        .put("name", item.getName())
                        .put("price", item.getPrice())
                ).collect(Collectors.toList());
    }
}
