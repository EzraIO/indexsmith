package es.model;

import lombok.Data;

/* *
 * */
@Data
public class CompensationInfos {

    private Long issueId;

    private Integer issueState;

    private Integer issueAuditStatus;

    private Integer overtTimePay;

    private Integer reasonType;
}
