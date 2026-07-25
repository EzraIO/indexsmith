package es.model;

import lombok.Data;
import java.util.List;

/* *
 * */
@Data
public class SkuWideTableEsIndexModel {

    private Integer orderRiskStatus;

    private Long orderViewId;

    private Long collectTime;

    private Boolean sameCity;

    private String pkgAbnormalStatus;

    private Long deliveryTime;

    private Long signTime;

    private Long payTime;

    private Integer orderStatus;

    private String logisticsCompanyCode;

    private Long userId;

    private Boolean fakePackage;

    private Long dispatchTime;

    private Boolean preSale;

    private Long payDeliveryGap;

    private Long payCollectGap;

    private List<Long> promiseDeliveryTime;

    private String pkgStatus;

    private Long paySignGap;

    private String waybillNo;

    private CompensationInfos compensationInfos;

    private SendInfo sendInfo;

    private List<ProductInfos> productInfos;

    private OrderRefundInfos orderRefundInfos;

    private SameCityInfo sameCityInfo;

    private MerchantInfo merchantInfo;

    private RecipientInfo recipientInfo;
}
