package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostLikeResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.entity.PostLike;
import com.fromm.leafmap.domain.post.repository.PostLikeRepository;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.apiPayload.exception.handler.ErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    @Override
    @Transactional
    public PostLikeResponseDTO.PostLikeResultDTO toggleLike(BoardType boardType, Long postId, Member member) {

        Post post = postRepository.findByIdAndBoardType(postId, boardType)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.POST_NOT_FOUND));

        // 이미 추천 눌렀는지 확인
        Optional<PostLike> existing = postLikeRepository.findByPostIdAndMemberId(postId, member.getId());

        boolean isLiked;

        if (existing.isPresent()) {
            // 추천 취소
            postLikeRepository.delete(existing.get());
            post.decreaseLike();
            isLiked = false;
        } else {
            // 추천 추가
            PostLike like = PostLike.builder()
                    .post(post)
                    .member(member)
                    .build();

            postLikeRepository.save(like);
            post.increaseLike();
            isLiked = true;
        }

        return PostLikeResponseDTO.PostLikeResultDTO.builder()
                .postId(postId)
                .likeCount(post.getLikeCount())
                .isLiked(isLiked)
                .build();
    }
}
