package ru.rostford.littleinfobot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@AllArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "user_id")
    private long userId;
    @Column(name = "last_founded_id")
    private int lastFoundedId;
    @Column(name = "is_moderator")
    private boolean isModerator;
    @Column(name = "current_action")
    private String currentAction;

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    private TempPerson tempPerson;

    public User() {
        this.lastFoundedId = 0;
        this.isModerator = true;
        this.currentAction = CurrentAction.NOTHING;
    }

    public final class CurrentAction {
        public static final String ADD_LAST_NAME = "add_last_name";
        public static final String ADD_FIRST_NAME = "add_first_name";
        public static final String ADD_MIDDLE_NAME = "add_middle_name";
        public static final String ADD_INFO = "add_info";
        public static final String EDIT_LAST_USER = "edit";
        public static final String NOTHING = "nothing";
    }
}
