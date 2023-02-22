package com.project.dailyAuction.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class BoardDto {
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Post {
        private String title;
        private String description;
        private String image;
        private long categoryId;
        private int startingPrice;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private long boardId;
        private long authorId;
        private long bidderId;
        private String title;
        private String description;
        private String image;
        private String thumbnail;
        private long statusId;
        private long categoryId;
        private int viewCount;
        private int bidCount;
        private int startingPrice;
        private int currentPrice;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
        private Integer[] history;
        private int myPrice;

        public void updateMyPrice(int myPrice) {
            this.myPrice = myPrice;
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MiniResponse {
        private String title;
        private String description;
        private String image;
        private String status;
        private int startingPrice;
        private int currentPrice;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Patch {
        private int price;
    }
}
