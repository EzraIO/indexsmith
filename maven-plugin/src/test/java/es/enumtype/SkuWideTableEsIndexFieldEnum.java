package es.enumtype;

/* *
 * */
public enum SkuWideTableEsIndexFieldEnum {

    ORDER_RISK_STATUS(0, "orderRiskStatus", "integer"),
    ORDER_VIEW_ID(1, "orderViewId", "long"),
    COLLECT_TIME(2, "collectTime", "long"),
    SAME_CITY(3, "sameCity", "boolean"),
    PKG_ABNORMAL_STATUS(4, "pkgAbnormalStatus", "keyword"),
    DELIVERY_TIME(5, "deliveryTime", "long"),
    SIGN_TIME(6, "signTime", "long"),
    PAY_TIME(7, "payTime", "long"),
    ORDER_STATUS(8, "orderStatus", "integer"),
    LOGISTICS_COMPANY_CODE(9, "logisticsCompanyCode", "keyword"),
    USER_ID(10, "userId", "long"),
    FAKE_PACKAGE(11, "fakePackage", "boolean"),
    DISPATCH_TIME(12, "dispatchTime", "long"),
    PRE_SALE(13, "preSale", "boolean"),
    PAY_DELIVERY_GAP(14, "payDeliveryGap", "long"),
    PAY_COLLECT_GAP(15, "payCollectGap", "long"),
    PROMISE_DELIVERY_TIME(16, "promiseDeliveryTime", "long"),
    PKG_STATUS(17, "pkgStatus", "keyword"),
    PAY_SIGN_GAP(18, "paySignGap", "long"),
    WAYBILL_NO(19, "waybillNo", "keyword"),
    COMPENSATION_INFOS(20, "compensationInfos", "nested"),
    COMPENSATION_INFOS_ISSUE_ID(21, "compensationInfos.issueId", "long"),
    COMPENSATION_INFOS_ISSUE_STATE(22, "compensationInfos.issueState", "integer"),
    COMPENSATION_INFOS_ISSUE_AUDIT_STATUS(23, "compensationInfos.issueAuditStatus", "integer"),
    COMPENSATION_INFOS_OVERT_TIME_PAY(24, "compensationInfos.overtTimePay", "integer"),
    COMPENSATION_INFOS_REASON_TYPE(25, "compensationInfos.reasonType", "integer"),
    SEND_INFO(26, "sendInfo", "null"),
    SEND_INFO_CITY_CODE(27, "sendInfo.cityCode", "keyword"),
    SEND_INFO_PROVINCE_CODE(28, "sendInfo.provinceCode", "keyword"),
    PRODUCT_INFOS(29, "productInfos", "nested"),
    PRODUCT_INFOS_SPU_ID(30, "productInfos.spuId", "long"),
    PRODUCT_INFOS_SKU_ID(31, "productInfos.skuId", "long"),
    ORDER_REFUND_INFOS(32, "orderRefundInfos", "nested"),
    ORDER_REFUND_INFOS_SERVICE_TYPE(33, "orderRefundInfos.serviceType", "integer"),
    ORDER_REFUND_INFOS_CTIME(34, "orderRefundInfos.ctime", "long"),
    ORDER_REFUND_INFOS_AFTER_SALE_ID(35, "orderRefundInfos.afterSaleId", "long"),
    ORDER_REFUND_INFOS_STATUS(36, "orderRefundInfos.status", "integer"),
    SAME_CITY_INFO(37, "sameCityInfo", "null"),
    SAME_CITY_INFO_COLLECT_TIME_RISK(38, "sameCityInfo.collectTimeRisk", "boolean"),
    SAME_CITY_INFO_ARRIVE_TIME_TYPE(39, "sameCityInfo.arriveTimeType", "integer"),
    SAME_CITY_INFO_GOV_SAME_CITY_TIME_OUT(40, "sameCityInfo.govSameCityTimeOut", "boolean"),
    SAME_CITY_INFO_SIGN_TIME_OUT(41, "sameCityInfo.signTimeOut", "boolean"),
    SAME_CITY_INFO_PROMISE_ARRIVE_TIME(42, "sameCityInfo.promiseArriveTime", "long"),
    MERCHANT_INFO(43, "merchantInfo", "null"),
    MERCHANT_INFO_BIZ_TYPE(44, "merchantInfo.bizType", "integer"),
    MERCHANT_INFO_MERCHANT_ID(45, "merchantInfo.merchantId", "long"),
    MERCHANT_INFO_POI_TEST_TAG(46, "merchantInfo.poiTestTag", "keyword"),
    MERCHANT_INFO_MERCHANT_NAME(47, "merchantInfo.merchantName", "text"),
    RECIPIENT_INFO(48, "recipientInfo", "null"),
    RECIPIENT_INFO_DISTRICT_CODE(49, "recipientInfo.districtCode", "keyword"),
    RECIPIENT_INFO_CITY_CODE(50, "recipientInfo.cityCode", "keyword"),
    RECIPIENT_INFO_PROVINCE_CODE(51, "recipientInfo.provinceCode", "keyword");

    private SkuWideTableEsIndexFieldEnum(int code, String esFieldPathName, String esFieldType) {
        this.code = code;
        this.esFieldPathName = esFieldPathName;
        this.esFieldType = esFieldType;
    }

    private int code;

    public int getCode() {
        return code;
    }

    private String esFieldPathName;

    public String getEsFieldPathName() {
        return esFieldPathName;
    }

    private String esFieldType;

    public String getEsFieldType() {
        return esFieldType;
    }
}
