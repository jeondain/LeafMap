package com.fromm.leafmap.domain.major.controller;

import com.fromm.leafmap.domain.major.service.MajorService;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/majors")
public class MajorController {

    private final MajorService majorService;

    @PostMapping(value = "/{majorId}/curriculumUrl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "전공 이수체계도 이미지 업로드 (연동 X)")
    public ApiResponse<String> uploadMajorImage( @PathVariable Long majorId,
                                                 @RequestPart("image") MultipartFile image) {
        String imageUrl = majorService.uploadCurriculumUrl(majorId, image);
        return ApiResponse.onSuccess(imageUrl);
    }
}
