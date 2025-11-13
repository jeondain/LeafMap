package com.fromm.leafmap.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSignupDTO {
        @NotNull(message = "id는 필수입니다.")
        private String loginId;
        @NotNull(message = "비밀번호는 필수입니다.")
        private String password;
        private String nickname;
        private String studentId;
        private String major;
        private String desiredMajor;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberLoginDTO {
        private String loginId;
        private String password;
    }
}
