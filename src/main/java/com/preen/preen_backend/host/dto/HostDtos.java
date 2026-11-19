package com.preen.preen_backend.host.dto;





/** host 요청·응답 모음. */
public class HostDtos {
    private HostDtos(){
    }



    /** 다른 feature가 소유 여부 검사에 쓰는 요약. */
    public record BusinessInfo(Long businessId, Long hostUserId){
    }

}
