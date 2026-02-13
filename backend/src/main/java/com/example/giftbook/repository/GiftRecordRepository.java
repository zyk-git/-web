package com.example.giftbook.repository;

import com.example.giftbook.entity.GiftRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftRecordRepository extends JpaRepository<GiftRecord, Long> {
    List<GiftRecord> findByNameContainingOrPayerNameContainingOrderBySubmitTimeDesc(String name, String payerName);
    List<GiftRecord> findAllByOrderBySubmitTimeDesc();
}
