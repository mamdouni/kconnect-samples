package org.example.json.transformers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ItemDTO {

    @JsonProperty("item_id")
    private Integer itemId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("price")
    private Integer price;
}
