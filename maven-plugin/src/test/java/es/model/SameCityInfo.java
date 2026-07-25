package es.model;

import lombok.Data;

/* *
 * */
@Data
public class SameCityInfo {

    private Boolean collectTimeRisk;

    private Integer arriveTimeType;

    private Boolean govSameCityTimeOut;

    private Boolean signTimeOut;

    private Long promiseArriveTime;
}
