package com.fps.svmes.models.sql.taskSchedule;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fps.svmes.models.sql.user.User;
import jakarta.persistence.*;
import lombok.Data;

import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_user", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    @JsonBackReference
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Integer status = 1; // Active by default

    public DispatchUser(Dispatch dispatch, Integer userId) {
        this.dispatch = dispatch;
        user = new User();
        user.setId(userId);
        this.setUser(user);
    }


    @Override
    public String toString() {
        return "DispatchUser{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }

}

