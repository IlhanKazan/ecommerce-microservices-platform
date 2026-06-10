package com.ecommerce.productservice.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_vote_per_user",
                columnNames = {"review_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Boolean isHelpful;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime votedAt = LocalDateTime.now();
}
