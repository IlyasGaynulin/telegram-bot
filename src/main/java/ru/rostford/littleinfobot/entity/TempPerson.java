package ru.rostford.littleinfobot.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "temp_person")
public class TempPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "middle_name")
    private String middleName;

    private String info;

    @Column(name = "creating_date")
    private Date creatingDate;

    public TempPerson() {
        this.creatingDate = new Date();
    }

    public Person toPerson() {
        Person person = new Person();
        person.setFirstName(this.firstName);
        person.setLastName(this.lastName);
        person.setMiddleName(this.middleName);
        person.setInfo(this.info);
        return person;
    }
}
