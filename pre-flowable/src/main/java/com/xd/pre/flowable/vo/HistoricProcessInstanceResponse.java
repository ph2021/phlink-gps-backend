package com.xd.pre.flowable.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * @author
 * @date
 */
@Data
public class HistoricProcessInstanceResponse {
    protected String id;
    protected String name;
    private String businessKey;
    protected String processDefinitionId;
    private String processDefinitionName;
    private String processDefinitionKey;
    private Integer processDefinitionVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    private Long durationInMillis;
    private String startUserId;
    private String startActivityId;
    private String superProcessInstanceId;
    protected String tenantId;
}
