package com.expensemate.performance;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class HibernateQueryCountSupport {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    protected Statistics statistics() {

        return entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    protected void resetQueryStatistics() {

        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    protected long preparedStatementCount() {

        return statistics()
                .getPrepareStatementCount();
    }
}