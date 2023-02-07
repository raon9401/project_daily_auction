package com.project.dailyAuction.board.repository;

import com.project.dailyAuction.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
    void deleteBySellerId(long sellerId);
}
