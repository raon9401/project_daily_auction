package com.project.dailyAuction.board.entity;

//import com.project.dailyAuction.boardNotice.entity.Notice;
import com.project.dailyAuction.boardImage.entity.BoardImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Board implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long boardId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column
    private long sellerId;
    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    private List<BoardImage> images = new ArrayList<>();
    @Column
    private String thumbnail;
    @Column
    private long statusId;
    @Column
    private long categoryId;
    @Column
    private int viewCount;
    @Column
    private int bidCount;
    @Column
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime finishedAt;
    @Column
    private int startingPrice;
    @Column
    private int currentPrice;
    @Column
    private long bidderId;
    @Column
    private String history;
//    @OneToMany(mappedBy = "board")
//    private List<Notice> notices = new ArrayList<>();

    public void changeLeadingBidder(long memberId) {
        this.bidderId = memberId;
    }

    public Long[] getHistoryArray(){
        String[] histories = this.history.split(",");
        return Arrays.stream(histories)
                .mapToLong(a -> Long.parseLong(a)).boxed()
                .toArray(Long[]::new);
    }

    public void updatePrice(int price) {
        this.currentPrice = price;
    }

//    public void updateHistory(int newPrice) {
//        this.history = this.history + "," + newPrice;
//    }

//    public void upBidCount(){
//        this.bidCount++;
//    }

    public void upViewCount(int viewCount) {
        this.viewCount += viewCount;
    }

    public void changeStatus(long statusId){
        this.statusId = statusId;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setImages(List<BoardImage> images) {
        this.images = images;
    }
}
