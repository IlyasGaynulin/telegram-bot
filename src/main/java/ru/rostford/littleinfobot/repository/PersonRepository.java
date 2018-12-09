package ru.rostford.littleinfobot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rostford.littleinfobot.entity.Person;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {
    Person findById(int id);
    @Query("select p from Person p where concat(concat(p.lastName, p.firstName), p.middleName) = :text")
    Person findByFullInfo(@Param("text") String text);
}
