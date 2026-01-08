package com.menekseyuncu.fundsearchservice.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "fund")
public class FundDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String fundCode;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String fundName;

    @Field(type = FieldType.Keyword)
    private String umbrellaType;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return1Month;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return3Month;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return6Month;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal returnYtd;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return1Year;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return3Year;

    @Field(type = FieldType.Scaled_Float)
    private BigDecimal return5Year;
}
