package com.project.dailyAuction.board.service;

import com.project.dailyAuction.board.Dto.BoardDto;
import com.project.dailyAuction.board.entity.Board;
import com.project.dailyAuction.board.repository.BoardRepository;
import com.project.dailyAuction.boardMember.entity.BoardMember;
import com.project.dailyAuction.boardMember.repository.BoardMemberRepository;
import com.project.dailyAuction.code.ExceptionCode;
import com.project.dailyAuction.member.entity.Member;
import com.project.dailyAuction.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoardService {
    private final BoardRepository boardRepository;
    private final MemberService memberService;
    private final BoardMemberRepository boardMemberRepository;
    private final RedisTemplate redisTemplate;

    public Board saveBoard(String token, BoardDto.Post postDto) {
        Member member = memberService.findByAccessToken(token);
        Board createdBoard = Board.builder()
                .title(postDto.getTitle())
                .description(postDto.getDescription())
                //todo: 이미지 변환 필요, 썸네일 생성 필요
                .image(postDto.getImage())
                .thumbnail("")
                .statusId(0)
                .category(postDto.getCategory())
                .createdAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now().plusDays(1))
                .startingPrice(postDto.getStarting_price())
                .sellerId(member.getMemberId())
                .history(String.valueOf(postDto.getStarting_price()))
                .build();

        return boardRepository.save(createdBoard);
    }

    public BoardDto.Response getDetailPage(String token, long boardId) {
        Board target = find(boardId);

        String[] history = target.getHistory().split(",");

        //조회수 증가
        //todo: redis로 처리하기
        target.upViewCount();

        BoardDto.Response response = BoardDto.Response.builder()
                .boardId(boardId)
                .title(target.getTitle())
                .description(target.getDescription())
                .category(target.getCategory())
                .image(target.getImage())
                .thumbnail(target.getThumbnail())
                .startingPrice(target.getStartingPrice())
                .currentPrice(target.getCurrentPrice())
                .createdAt(target.getCreatedAt())
                .finishedAt(target.getFinishedAt())
                .viewCount(target.getViewCount())
                .bidCount(target.getBidCount())
                .history(target.getHistoryList())
                .statusId(target.getStatusId())
                .bidderId(target.getBidderId())
                .sellerId(target.getSellerId())
                .build();

        if (token!=null){
            Member member = memberService.findByAccessToken(token);
            //내 가격 업데이트
            response.updateMyPrice(findMyPrice(member, target));
        }
        return response;
    }

//    @Cacheable(value = "board")
    public void addViewCntToRedis(long boardId){
        String key = "productViewCnt::"+boardId;
        //hint 캐시에 값이 없으면 레포지토리에서 조회 있으면 값을 증가시킨다.
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if(valueOperations.get(key)==null)
            valueOperations.set(
                    key,
                    String.valueOf(boardRepository.findViewCountByBoardId(boardId)),
                    Duration.ofMinutes(5));
        else
            valueOperations.increment(key);
        log.info("value:{}",valueOperations.get(key));
    }

    public void deleteBoard(String token, long boardId) {
        Board target = find(boardId);
        if (target.getSellerId() != memberService.findByAccessToken(token).getMemberId()) {
            new ResponseStatusException(ExceptionCode.NOT_WRITER.getCode(),
                    ExceptionCode.NOT_WRITER.getMessage(),
                    new IllegalArgumentException());
        }
        boardRepository.delete(target);
    }


    public void bidBoard(String token, BoardDto.Patch patchDto) {
        Member member = memberService.findByAccessToken(token);
        Board board = find(patchDto.getBoardId());
        int currentPrice = board.getCurrentPrice();
        int newPrice = patchDto.getNewPrice();
        if (board.getBidderId()!=0){
            Member lastMember = memberService.find(board.getBidderId());
            //코인 증가
            lastMember.changeCoin(currentPrice);
        }
        //코인이 부족하면 에러
        if (member.getCoin() < newPrice) {
            new ResponseStatusException(ExceptionCode.NOT_ENOUGH_COIN.getCode(),
                    ExceptionCode.NOT_ENOUGH_COIN.getMessage(),
                    new IllegalArgumentException());
        }

        //입찰가보다 낮거나 같으면 에러
        if (currentPrice >= newPrice) {
            new ResponseStatusException(ExceptionCode.LESS_THAN_CURRENT.getCode(),
                    ExceptionCode.LESS_THAN_CURRENT.getMessage(),
                    new IllegalArgumentException());
        }
        board.changeLeadingBidder(member.getMemberId(), newPrice);
        //bid count 증가
        board.upBidCount();
        //히스토리 추가
        board.updateHistory(newPrice);

        //기록용 남기기
        boardMemberRepository.save(BoardMember.builder()
                .board(board)
                .member(member)
                .myPrice(newPrice)
                .build());

        //코인 감소
        member.changeCoin(-newPrice);
    }

    public Board find(long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(ExceptionCode.BOARD_NOT_FOUND.getCode(),
                        ExceptionCode.BOARD_NOT_FOUND.getMessage(),
                        new IllegalArgumentException()));
    }

    public int findMyPrice(Member member, Board board) {
        BoardMember boardMember = boardMemberRepository.findByBoardAndMember(board, member);
        return boardMember.getMyPrice();
    }
}
