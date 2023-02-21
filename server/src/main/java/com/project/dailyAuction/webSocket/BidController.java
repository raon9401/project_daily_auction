package com.project.dailyAuction.webSocket;

import com.project.dailyAuction.board.Dto.BoardDto;
import com.project.dailyAuction.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Arrays;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BidController {
    private final BoardService boardService;
    private final SimpMessageSendingOperations simpMessageSendingOperations;

    @MessageMapping("/bid")
    public void bidMessage(Message.Bid message) {
        long boardId = message.getBoardId();
        String token = message.getBidderToken();
        int newPrice = message.getPrice();

        boardService.bidBoard(token, boardId, newPrice);

        int bidCount = boardService.getBidCountInRedis(boardId);
        String history = boardService.getHistoryInRedis(boardId);
        Integer[] histories = Arrays.stream(history.split(","))
                .mapToInt(Integer::parseInt).boxed().toArray(Integer[]::new);

        Message.Response response = Message.Response.builder()
                .boardId(boardId)
                .bidCount(bidCount)
                .history(histories)
                .currentPrice(message.getPrice())
                .build();
        simpMessageSendingOperations.convertAndSend("/sub/board-id/" + message.getBoardId(), response);
    }

    @MessageMapping("/init")
    public void initMessage(Message.Init message) {
        long boardId = message.getBoardId();
        String token = null;

        int bidCount = boardService.getBidCountInRedis(boardId);

        String history = boardService.getHistoryInRedis(boardId);
        int currentPrice = boardService.getPriceInRedis(boardId);
        long bidderId = boardService.getBidderInRedis(boardId);
        int viewCount = boardService.addViewCntToRedis(boardId);

        int myPrice = 0;
        if (!message.getBidderToken().equals("")) {
            token = message.getBidderToken();
            myPrice = boardService.findMyPrice(token, boardId);

        }
        BoardDto.Response dto = boardService.getDetailPage(token, boardId, currentPrice, viewCount, bidCount, bidderId, history);

        Message.InitResponse response = Message.InitResponse.builder()
                .boardId(boardId)
                .bidCount(bidCount)
                .currentPrice(dto.getCurrentPrice())
                .history(dto.getHistory())
                .myPrice(myPrice)
                .build();


        simpMessageSendingOperations.convertAndSend("/sub/board-id/" + message.getBoardId(), response);
    }
}
