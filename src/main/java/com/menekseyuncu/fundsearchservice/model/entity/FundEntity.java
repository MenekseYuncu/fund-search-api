package com.menekseyuncu.fundsearchservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funds")
public class FundEntity {

    @Id
    private String fundCode;

    private String fundName;

    private String umbrellaType;

    @Column(precision = 20, scale = 4)
    private BigDecimal return1Month;

    @Column(precision = 20, scale = 4)
    private BigDecimal return3Month;

    @Column(precision = 20, scale = 4)
    private BigDecimal return6Month;

    @Column(precision = 20, scale = 4)
    private BigDecimal returnYtd;

    @Column(precision = 20, scale = 4)
    private BigDecimal return1Year;

    @Column(precision = 20, scale = 4)
    private BigDecimal return3Year;

    @Column(precision = 20, scale = 4)
    private BigDecimal return5Year;
}
