package com.fromm.leafmap.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
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
        @NotBlank(message = "id는 필수입니다.")
        private String loginId;
        @NotBlank(message = "비밀번호는 필수입니다.")
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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMemberInfoDTO {
        private String nickname;
        private String major;
        private String desiredMajor;
    }
}
