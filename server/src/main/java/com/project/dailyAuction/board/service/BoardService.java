package com.project.dailyAuction.board.service;

import com.project.dailyAuction.board.dto.BoardDto;
import com.project.dailyAuction.board.entity.Board;
import com.project.dailyAuction.board.repository.BoardRepository;
import com.project.dailyAuction.boardImage.entity.BoardImage;
import com.project.dailyAuction.boardImage.repository.BoardImageRepository;
import com.project.dailyAuction.boardMember.entity.BoardMember;
import com.project.dailyAuction.boardMember.repository.BoardMemberRepository;
import com.project.dailyAuction.cache.CacheProcessor;
import com.project.dailyAuction.code.ExceptionCode;
import com.project.dailyAuction.code.NoticeStatusCode;
import com.project.dailyAuction.member.entity.Member;
import com.project.dailyAuction.member.service.MemberService;
import com.project.dailyAuction.notice.Notice;
import com.project.dailyAuction.notice.NoticeRepository;
import com.project.dailyAuction.notice.NoticeService;
import com.project.dailyAuction.webSocket.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoardService {
    private final BoardRepository boardRepository;
    private final NoticeService noticeService;
    private final MemberService memberService;
    private final BoardMemberRepository boardMemberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NoticeRepository noticeRepository;
    private final CacheProcessor cacheProcessor;
    private final BoardImageRepository boardImageRepository;

    public Board saveBoard(String token, BoardDto.Post postDto) {
        Member member = memberService.findByAccessToken(token);
        Board createdBoard = Board.builder()
                .title(postDto.getTitle())
                .description(postDto.getDescription())
                .thumbnail("")
                .statusId(1)
                .categoryId(postDto.getCategoryId())
                .createdAt(LocalDateTime.now().plusHours(9))
                .finishedAt(LocalDateTime.now().plusHours(33))
                .sellerId(member.getMemberId())
                .startingPrice(postDto.getStartingPrice())
                .currentPrice(postDto.getStartingPrice())
                .sellerId(member.getMemberId())
                .history(String.valueOf(postDto.getStartingPrice()))
                .build();

        return boardRepository.save(createdBoard);
    }

    public BoardDto.Response getDetailPage(String token, Board board, int currentPrice, int viewCount, int bidCount, long bidderId, String history) {

        Integer[] histories = Arrays.stream(history.split(","))
                .mapToInt(Integer::parseInt).boxed().toArray(Integer[]::new);


        BoardDto.Response response = BoardDto.Response.builder()
                .boardId(board.getBoardId())
                .title(board.getTitle())
                .description(board.getDescription())
                .categoryId(board.getCategoryId())
                .imageUrls(findImageUrls(board))
                .thumbnail(board.getThumbnail())
                .startingPrice(board.getStartingPrice())
                .currentPrice(currentPrice)
                .createdAt(board.getCreatedAt())
                .finishedAt(board.getFinishedAt())
                .viewCount(viewCount)
                .bidCount(bidCount)
                .history(histories)
                .statusId(board.getStatusId())
                .bidderId(bidderId)
                .authorId(board.getSellerId())
                .build();

        if (token != null) {
            Member member = memberService.findByAccessToken(token);
            //내 가격 업데이트
            response.updateMyPrice(findMyPrice(token, board.getBoardId()));

            //유저가 board상세페이지에 접속하려고하면 알림의 상태를 읽음으로 바꾼다.
            List<Notice> notices = noticeRepository.findAllByReceiverAndBoard(member, board);
            if (!notices.isEmpty()) {
                notices.forEach(
                        notice -> {
                            notice.read();
                        }
                );
            }
        }
        return response;
    }

    public int getViewCount(Board board, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        long boardId = board.getBoardId();
        int viewCount;
        // 조회수 로직
        Cookie oldCookie = null;
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("postView")) {
                    oldCookie = cookie;
                }
            }
        }

        if (oldCookie != null) { // 쿠키 묶음이 있는 경우 -> 체크 후 증가
            if (!oldCookie.getValue().contains("[" + boardId + "]")) {  // 그 중에서 해당 보드 쿠키가 없는 경우
                viewCount = addViewCntToRedis(board);
                oldCookie.setValue(oldCookie.getValue() + "_[" + boardId + "]");
                oldCookie.setPath("/");
                oldCookie.setMaxAge(60 * 60 * 24);
                httpResponse.addCookie(oldCookie);
            } else {  // 해당 보드 쿠키는 있는 경우
                viewCount = getViewCntToRedis(board);
            }
        } else { // 쿠키 묶음이 없는 경우 -> 무조건 증가
            viewCount = addViewCntToRedis(board);
            Cookie newCookie = new Cookie("postView", "[" + boardId + "]");
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24);
            httpResponse.addCookie(newCookie);
        }
        return viewCount;
    }

    public int getViewCntToRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardViewCount::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            valueOperations.set(
                    key,
                    String.valueOf(board.getViewCount()));
            return Integer.parseInt(valueOperations.get(key));
        } else {
            valueOperations.get(key);
            return Integer.parseInt(valueOperations.get(key));
        }
    }

    public int addViewCntToRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardViewCount::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            valueOperations.set(
                    key,
                    String.valueOf(board.getViewCount() + 1));
        } else {
            valueOperations.increment(key);
        }
        return Integer.parseInt(valueOperations.get(key));
    }

    public int addBidCountToRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardBidCount::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) { // 키가 없으면 보드에서 가져와서 +1
            valueOperations.set(
                    key,
                    String.valueOf(board.getBidCount() + 1));
        } else {  // 키가 있으면 1 증가
            valueOperations.increment(key);
        }
        return Integer.parseInt(valueOperations.get(key));
    }

    public String addHistoryToRedis(Board board, int newPrice) {
        long boardId = board.getBoardId();
        String key = "boardHistory::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            valueOperations.set(
                    key,
                    board.getHistory() + "," + newPrice);
        } else {
            String lastHistory = valueOperations.get(key);
            valueOperations.set(key, lastHistory + "," + newPrice);
        }
        return valueOperations.get(key);
    }

    private void changeLeadingBidderToRedis(long boardId, long bidderId) {
        String key = "boardLeadingBidder::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, String.valueOf(bidderId));
    }

    public int getBidCountInRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardBidCount::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            return board.getBidCount();
        } else {
            return Integer.parseInt(valueOperations.get(key));
        }
    }

    public long getBidderInRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardLeadingBidder::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            return board.getBidderId();
        } else {
            return Long.parseLong(valueOperations.get(key));
        }
    }

    public String getHistoryInRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardHistory::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            return board.getHistory();
        } else {
            return valueOperations.get(key);
        }
    }

    public int getPriceInRedis(Board board) {
        long boardId = board.getBoardId();
        String key = "boardPrice::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            return board.getCurrentPrice();
        } else {
            return Integer.parseInt(valueOperations.get(key));
        }
    }

    public void setFinishedTimeToRedis(Board board, LocalDateTime finishedTime) {
        long boardId = board.getBoardId();
        String key = "finishedTime::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (valueOperations.get(key) == null) {
            String parsedFinishedAt = board.getFinishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            valueOperations.set(
                    key,
                    parsedFinishedAt);
        } else {
            String parsedFinishedAt = finishedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            valueOperations.set(key, parsedFinishedAt);
        }
    }

    public void deleteBoard(String token, long boardId) {
        Board target = find(boardId);
        if (target.getSellerId() != memberService.findByAccessToken(token).getMemberId()) {
            throw new ResponseStatusException(ExceptionCode.NOT_WRITER.getCode(),
                    ExceptionCode.NOT_WRITER.getMessage(),
                    new IllegalArgumentException());
        }
        // 경매 진행 중일 때 삭제하면 환불
        if (target.getStatusId() == 1) {
            Member lastBidder = memberService.find(target.getBidderId());
            lastBidder.changeCoin(target.getCurrentPrice());
        }

        boardRepository.delete(target);
    }


    public Message.Response bidBoard(String token, long boardId, int newPrice) {
        Member member = memberService.findByAccessToken(token);
        Board board = find(boardId);

        // 자기글에 입찰 불가
        if (member.getMemberId() == board.getSellerId()) {
            throw new ResponseStatusException(ExceptionCode.CANT_BID_SELF.getCode(),
                    ExceptionCode.CANT_BID_SELF.getMessage(),
                    new IllegalArgumentException());
        }

        int currentPrice = getPriceInRedis(board);

        if (board.getBidderId() != 0) {
            Member lastMember = memberService.find(board.getBidderId());
            //코인 증가
            lastMember.changeCoin(currentPrice);

            //알림 발송
            noticeService.send(lastMember, board, NoticeStatusCode.상회입찰.getCode(), lastMember.getCoin());
        }

        //코인이 부족하면 에러
        if (member.getCoin() < newPrice) {
            throw new ResponseStatusException(ExceptionCode.NOT_ENOUGH_COIN.getCode(),
                    ExceptionCode.NOT_ENOUGH_COIN.getMessage(),
                    new IllegalArgumentException());
        }

        //입찰가보다 낮거나 같으면 에러
        if (currentPrice >= newPrice) {
            throw new ResponseStatusException(ExceptionCode.LESS_THAN_CURRENT.getCode(),
                    ExceptionCode.LESS_THAN_CURRENT.getMessage(),
                    new IllegalArgumentException());
        }
        //리딩비더 변경
        if (board.getBidderId() == 0) {
            board.changeLeadingBidder(member.getMemberId());
        } else {
            changeLeadingBidderToRedis(board.getBoardId(), member.getMemberId());
        }

        //현재입찰가 변경
        if (board.getCurrentPrice() == 0) {
            board.updatePrice(newPrice);
        } else {
            changePriceToRedis(board.getBoardId(), newPrice);
        }

        //bid count 증가
        int bidCount = addBidCountToRedis(board);
        //히스토리 추가
        String history = addHistoryToRedis(board, newPrice);

        //기록용 남기기
        Optional<BoardMember> optionalBoardMember = boardMemberRepository.findByBoardAndMember(board, member);
        if (optionalBoardMember.isEmpty()) {
            boardMemberRepository.save(BoardMember.builder()
                    .board(board)
                    .member(member)
                    .myPrice(newPrice)
                    .build());
        } else {
            optionalBoardMember.get().changeMyPrice(newPrice);
        }

        //코인 감소
        member.changeCoin(-newPrice);

        // 기록 배열로 변환
        Integer[] histories = Arrays.stream(history.split(","))
                .mapToInt(Integer::parseInt).boxed().toArray(Integer[]::new);

        return Message.Response.builder()
                .boardId(boardId)
                .bidCount(bidCount)
                .history(histories)
                .currentPrice(newPrice)
                .build();
    }

    private void changePriceToRedis(long boardId, int newPrice) {
        String key = "boardPrice::" + boardId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        valueOperations.set(key, String.valueOf(newPrice));
    }

    public Board find(long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(ExceptionCode.BOARD_NOT_FOUND.getCode(),
                        ExceptionCode.BOARD_NOT_FOUND.getMessage(),
                        new IllegalArgumentException()));
    }

    public List<Board> getPopularItem(long categoryId) {
        cacheProcessor.updateViewCntToMySql();
        if (categoryId == 0) {
            return boardRepository.findTop5ByStatusIdOrderByViewCountDesc(1);
        } else {
            return boardRepository.findTop5ByCategoryIdAndStatusIdOrderByViewCountDesc(categoryId, 1);
        }
    }

    public List<Board> getImminentItem() {
        return boardRepository.findTop5ByStatusIdOrderByCreatedAtDesc(1);
    }

    public int findMyPrice(String token, long boardId) {
        Board board = find(boardId);
        Member member = memberService.findByAccessToken(token);
        Optional<BoardMember> optionalBoardMember = boardMemberRepository.findByBoardAndMember(board, member);
        if (optionalBoardMember.isEmpty()) {
            return 0;
        } else {
            return optionalBoardMember.get().getMyPrice();
        }
    }

    public Page<Board> findBoardPage(long categoryId, int page, int size, int sort) {
        Sort defaultSort = Sort.by("boardId").descending();

        if (sort == 1) {//마감임박순 정렬
            defaultSort = Sort.by("createdAt").ascending();
        } else if (sort == 2) {//입찰수 기준 정렬
            cacheProcessor.updateBiddingToMySql();
            defaultSort = Sort.by("bidCount").descending();
        } else if (sort == 3) {//조회수 기준 정렬
            cacheProcessor.updateViewCntToMySql();
            defaultSort = Sort.by("viewCount").descending();
        } else if (sort == 4) {//높은 현재가 기준 정렬
            cacheProcessor.updateBoardPriceToMySql();
            defaultSort = Sort.by("currentPrice").descending();
        } else if (sort == 5) {//낮은 현재가 기준 정렬
            cacheProcessor.updateBoardPriceToMySql();
            defaultSort = Sort.by("currentPrice").ascending();
        }
        // 전체 리스트 조회
        if (categoryId == 0) {//최근 하루의 모든 경매 조회
            return boardRepository.getBoardsByCreatedAtAfter(LocalDateTime.now().minusDays(1), PageRequest.of(page, size, defaultSort));
        } else {//카테고리면 최근 하루의 경매 조회
            return boardRepository.findBoardsByCategoryIdAndCreatedAt(categoryId,
                    LocalDateTime.now().minusDays(1), PageRequest.of(page, size, defaultSort));
        }
    }

    public List<String> findImageUrls(Board board) {
        List<String> imageUrls = new ArrayList<>();
        List<BoardImage> boardImages = boardImageRepository.findAllByBoard(board);
        for (BoardImage boardImage : boardImages) {
            imageUrls.add(boardImage.getStoredFilePath());
        }

        return imageUrls;
    }
}
