package com.image_upload_utility.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "gepnic_general_auction_product_images")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {


	@Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name="productid")
    private Long productid;

    @Column(name="imagename")
    private String imagename;

    @Column(name="imagesize")
    private Double imagesize;

    @Column(name="mimetype")
    private String mimetype;

    @Column(name="signingcertid")
    private Long signingcertid;

    @Column(name="actualimage", columnDefinition = "TEXT")
    private String actualimage;

    @Column(name="thumbnailimage", columnDefinition = "TEXT")
    private String thumbnailimage;

    @Column(name="uploadedby")
    private Long uploadedby;

    @Column(name="uploadeddate")
    private Date uploadeddate;

    @Column(name="verifiedby")
    private Long verifiedby;

    @Column(name="verifieddate")
    private Date verifieddate;


}
