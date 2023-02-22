package com.project.dailyAuction.notice;

import com.project.dailyAuction.board.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeResponseDto {
    private long noticeId;
    private long boardId;
    private String boardTitle;
    private String image;
    private long statusId;
    private String contact;
    private int coin;

    public static NoticeResponseDto create(Notice notice) {
        Board board = notice.getBoard();
        return NoticeResponseDto.builder()
                .noticeId(notice.getNoticeId())
                .boardId(board.getBoardId())
                .boardTitle(board.getTitle())
                .image(board.getImage())
                .statusId(notice.getStatus())
                .contact(notice.getContact())
                .build();
    }

    //오버로드
    public static NoticeResponseDto create(Notice notice, int coin) {
        Board board = notice.getBoard();
        return NoticeResponseDto.builder()
                .noticeId(notice.getNoticeId())
                .boardId(board.getBoardId())
                .boardTitle(board.getTitle())
                .image(board.getImage())
                .statusId(notice.getStatus())
                .contact(notice.getContact())
                .coin(coin)
                .build();
    }
}
