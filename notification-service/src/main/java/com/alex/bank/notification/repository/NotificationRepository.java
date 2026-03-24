package com.alex.bank.notification.repository;

import com.alex.bank.notification.entity.Notification;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends CrudRepository<Notification,String>,CustomJdbcRepository {

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE notification_id=:notificationId)")
    boolean existsByNotificationId(String notificationId);
}
