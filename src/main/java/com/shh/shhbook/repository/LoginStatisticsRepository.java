package com.shh.shhbook.repository;

import com.shh.shhbook.model.LoginStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginStatisticsRepository extends JpaRepository<LoginStatistics, Long> {

    @Query(value = "SELECT * FROM login_statistics ORDER BY year DESC, month DESC LIMIT 3", nativeQuery = true)
    List<LoginStatistics> findLastThreeMonths();
}
