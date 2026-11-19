package com.preen.preen_backend.host.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "host_businesses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostBusiness {

    public enum ApprovalStatus{
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostUserId;


    @Column(nullable = false, length = 100)
    private String businessName;

    @Column(nullable = false, length = 20)
    private String businessRegistrationNo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;


    /** 1차는 심사 없이 바로 승인 상태로 만든다. approval_status는 이후 심사 기능을 위해 남겨 둔다. */
    public static HostBusiness approved(Long hostUserId, String businessName, String registrationNo,
                                        LocalDateTime now){
        HostBusiness business = new HostBusiness();
        business.hostUserId = hostUserId;
        business.businessName = businessName;
        business.businessRegistrationNo = registrationNo;
        business.approvalStatus = ApprovalStatus.APPROVED;
        business.createdAt = now;
        return  business;
    }





}
