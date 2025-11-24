package com.fromm.leafmap.domain.comment.service;

import com.fromm.leafmap.domain.comment.dto.CommentRequestDTO;
import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.comment.entity.Comment;
import com.fromm.leafmap.domain.comment.repository.CommentRepository;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.apiPayload.exception.handler.ErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public CommentResponseDTO.AddCommentResultDTO addComment(Long postId, CommentRequestDTO.AddCommentDTO request, Member member) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.POST_NOT_FOUND));

        // 부모 댓글 조회 (대댓글인 경우)
        Comment parent = null;
        if (request.getParentId() != 0) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.COMMENT_NOT_FOUND));

            // 부모 댓글이 해당 게시글에 속하지 않으면 예외
            if (!parent.getPost().getId().equals(postId)) {
                throw new ErrorHandler(ErrorStatus.INVALID_COMMENT_PARENT);
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .member(member)
                .parent(parent)
                .build();

        commentRepository.save(comment);
        return CommentResponseDTO.AddCommentResultDTO.builder()
                .commentId(comment.getId())
                .build();
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, Member member) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new ErrorHandler(ErrorStatus.POST_NOT_FOUND);
        }

        // 작성자 검증
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new ErrorHandler(ErrorStatus.COMMENT_NO_PERMISSION);
        }

        commentRepository.delete(comment);
    }
}
