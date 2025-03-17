package com.image_upload_utility.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "gepnic_general_auction_product_details")
@Data
public class ProductDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String productrefno;
    private String productname;
    private String description;
    private int unitmeasurementid;
    private BigDecimal productlength;
    private BigDecimal productwidth;
    private BigDecimal productheight;
    private BigDecimal productweight;
    private BigDecimal packagedweight;
    private String productstatus;
    private Long productcategoryid;
    private Long productsubcategoryid;
    private BigDecimal actualprice;
    private BigDecimal cgstpercentage;
    private BigDecimal sgstpercentage;
    private BigDecimal igstpercentage;
    private BigDecimal handlingcharge;
    private Boolean fragileitem;
    private Boolean featureitem;
    private String featureone;
    private String featuretwo;
    private String featurethree;
    private Long actualorgid;
    private String rootfolderpath;
    private Long rootfolderid;
    private Long createdby;
    private Timestamp createddate;
    private String updatedby;
    private Timestamp updateddate;
    private String productstage;
    private Boolean ispublished;
    private Long publishedby;
    private Timestamp publisheddate;
    private Long weightmeasurementid;
    private String productdeptcode;
    private String insuranceappl;
    private Long productdocid;
    private Boolean highlightitem;
}

