package ru.rostford.littleinfobot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rostford.littleinfobot.entity.User;

import javax.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findUserByUserId(long userId);
    @Modifying
    @Transactional
    @Query("update User user set user.currentAction = :currentAction where user.userId = :userId")
    void setCurrentAction(@Param("currentAction")String currentAction, @Param("userId") long userId);
}
