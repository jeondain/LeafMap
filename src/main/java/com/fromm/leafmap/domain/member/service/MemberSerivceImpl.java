package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.major.entity.Major;
import com.fromm.leafmap.domain.major.repository.MajorRepository;
import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.member.entity.Role;
import com.fromm.leafmap.domain.member.repository.MemberRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.apiPayload.exception.handler.ErrorHandler;
import com.fromm.leafmap.global.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberSerivceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MajorRepository majorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto) {
        Optional<Member> optionalMember = memberRepository.findByLoginId(memberSignupDto.getLoginId());

        if (optionalMember.isPresent()) {
           throw new ErrorHandler(ErrorStatus.MEMBER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(memberSignupDto.getPassword());

        Major major = majorRepository.findByName(memberSignupDto.getMajor())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));

        Major desiredMajor = majorRepository.findByName(memberSignupDto.getDesiredMajor())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));

        Member member = Member.builder()
                .loginId(memberSignupDto.getLoginId())
                .password(encodedPassword)
                .nickname(memberSignupDto.getNickname())
                .studentId(memberSignupDto.getStudentId())
                .major(major)
                .desiredMajor(desiredMajor)
                .role(Role.USER)
                .build();

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public MemberResponseDTO.MemberLoginResultDTO login(MemberRequestDTO.MemberLoginDTO memberLoginDto) {
        Member member = memberRepository.findByLoginId(memberLoginDto.getLoginId())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new ErrorHandler(ErrorStatus.INVALID_PASSWORD);
        }

        String accessToken = jwtService.createAccessToken(member.getLoginId());

        return MemberResponseDTO.MemberLoginResultDTO.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .build();
    }

    @Override
    @Transactional
    public MemberResponseDTO.GetMemberInfoResultDTO getMemberInfo(Member member) {
        return MemberResponseDTO.GetMemberInfoResultDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .major(member.getMajor() != null ? member.getMajor().getName() : null)
                .desiredMajor(member.getDesiredMajor() != null ? member.getDesiredMajor().getName() : null)
                .build();
    }
}
