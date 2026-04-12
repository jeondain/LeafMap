package com.fromm.leafmap.domain.major.service;

import com.fromm.leafmap.domain.major.entity.Major;
import com.fromm.leafmap.domain.major.repository.MajorRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.exception.handler.ErrorHandler;
import com.fromm.leafmap.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorRepository majorRepository;
    private final S3Uploader s3Uploader;

    public String uploadCurriculumUrl(Long majorId, MultipartFile image) {
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));

        String imageUrl = s3Uploader.upload(image, "major");

        major.setCurriculumUrl(imageUrl);
        majorRepository.save(major);

        return imageUrl;
    }
}

