package com.fromm.leafmap.domain.post.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "posts")
@Setting(settingPath = "/elasticsearch/settings.json")
@Mapping(mappingPath = "/elasticsearch/mappings.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String boardType;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String content;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String address;

    @Field(type = FieldType.Boolean)
    private Boolean badge;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String imageUrl;
}
